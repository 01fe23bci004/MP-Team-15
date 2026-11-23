package edu.bvb.campus;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import java.util.concurrent.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("demo") @AutoConfigureMockMvc
class EngineIntegrationTest {
 @Autowired EngineService service; @Autowired UserRepo users; @Autowired CategoryRepo cats; @Autowired SubcategoryRepo subs; @Autowired RequestRepo requests; @Autowired TechRepo techs; @Autowired AssignmentRepo assignments;
 @Autowired EventRepo eventRepo;
 @Autowired MockMvc mvc;
 @BeforeEach void clearWork(){eventRepo.deleteAll();assignments.deleteAll();requests.deleteAll();}
 @Test void concurrentAllocationNeverConfirmsTwiceForOneRequest() throws Exception {
   AppUser admin=users.findByUsername("admin").orElseThrow();AppUser requester=users.findByUsername("student").orElseThrow();
   ServiceCategory cat=cats.findAll().stream().filter(c->c.name.equals("Electrical")).findFirst().orElseThrow();ServiceSubcategory sub=subs.findAll().stream().filter(s->s.skill.equals("Electrical")).findFirst().orElseThrow();
   ServiceRequest r=new ServiceRequest();r.requester=requester;r.category=cat;r.subcategory=sub;r.title="Concurrency test";r.description="Simultaneous allocation";r.location="Main Block";r.priority=Priority.HIGH;r.status=RequestStatus.QUEUED;r=requests.saveAndFlush(r);
   final long id=r.id;ExecutorService pool=Executors.newFixedThreadPool(12);CountDownLatch start=new CountDownLatch(1);var results=new java.util.ArrayList<Future<Object>>();
   for(int i=0;i<12;i++)results.add(pool.submit(()->{start.await();try{return service.allocate(id,admin);}catch(RuntimeException ex){return ex;}}));start.countDown();for(var f:results)f.get(10,TimeUnit.SECONDS);pool.shutdownNow();
   assertEquals(1,assignments.findAll().stream().filter(a->a.request.id==id).count());assertEquals(RequestStatus.ASSIGNED,requests.findById(id).orElseThrow().status);
 }
 @Test void eligibilityRespectsSkillLocationAndCapacity() {
   AppUser electrical=users.findByUsername("tech.electrical").orElseThrow();TechnicianProfile p=techs.findById(electrical.id).orElseThrow();
   assertTrue(p.skills.contains("Electrical"));assertTrue(p.locations.contains("Main Block"));assertTrue(p.available);assertTrue(p.maxActive>0);
 }
 @Test void simultaneousSubmissionsDoNotOverbookTheOnlyEligibleTechnician() throws Exception {
   AppUser requester=users.findByUsername("student").orElseThrow();ServiceCategory cat=cats.findAll().stream().filter(c->c.name.equals("Electrical")).findFirst().orElseThrow();ServiceSubcategory sub=subs.findAll().stream().filter(s->s.skill.equals("Electrical")).findFirst().orElseThrow();
   ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
   var a=pool.submit(()->{start.await();return service.submit(requester,new RequestInput("Light A","Out","Main Block",cat.id,sub.id,null));});
   var b=pool.submit(()->{start.await();return service.submit(requester,new RequestInput("Light B","Out","Main Block",cat.id,sub.id,null));});start.countDown();ServiceRequest ra=a.get(10,TimeUnit.SECONDS),rb=b.get(10,TimeUnit.SECONDS);pool.shutdownNow();
   assertEquals(1,assignments.findAll().size());assertTrue(List.of(ra.status,rb.status).contains(RequestStatus.QUEUED));assertTrue(List.of(ra.status,rb.status).contains(RequestStatus.ASSIGNED));
 }
 @Test void intakeUsesCategoryPriorityAndLifecycleIsTraceable() {
   AppUser requester=users.findByUsername("student").orElseThrow();AppUser admin=users.findByUsername("admin").orElseThrow();
   ServiceCategory cat=cats.findAll().stream().filter(c->c.name.equals("Electrical")).findFirst().orElseThrow();ServiceSubcategory sub=subs.findAll().stream().filter(s->s.skill.equals("Electrical")).findFirst().orElseThrow();
   ServiceRequest r=service.submit(requester,new RequestInput("Priority test","Light is out","Main Block",cat.id,sub.id,null));
   assertEquals(Priority.HIGH,r.priority);assertEquals(RequestStatus.ASSIGNED,r.status);
   service.changeStatus(r.id,RequestStatus.ACCEPTED,"Accepted",users.findByUsername("tech.electrical").orElseThrow());
   service.changeStatus(r.id,RequestStatus.IN_PROGRESS,"Started",users.findByUsername("tech.electrical").orElseThrow());
   service.changeStatus(r.id,RequestStatus.COMPLETED,"Resolved",users.findByUsername("tech.electrical").orElseThrow());
   assertEquals(5,eventsFor(r.id));assertEquals(RequestStatus.COMPLETED,requests.findById(r.id).orElseThrow().status);
 }
 @Test void unauthorizedStatusReadAndInvalidTransitionAreRejected() throws Exception {
   AppUser requester=users.findByUsername("student").orElseThrow();AppUser faculty=users.findByUsername("faculty").orElseThrow();AppUser tech=users.findByUsername("tech.electrical").orElseThrow();
   ServiceCategory cat=cats.findAll().stream().filter(c->c.name.equals("Electrical")).findFirst().orElseThrow();ServiceSubcategory sub=subs.findAll().stream().filter(s->s.skill.equals("Electrical")).findFirst().orElseThrow();
   ServiceRequest r=service.submit(requester,new RequestInput("Private request","Details","Main Block",cat.id,sub.id,null));
   mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/requests/"+r.id).header("Authorization","Bearer "+service.issue(faculty)))
      .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
   assertThrows(RuntimeException.class,()->service.changeStatus(r.id,RequestStatus.IN_PROGRESS,"Skip acceptance",tech));
   assertEquals(RequestStatus.ASSIGNED,requests.findById(r.id).orElseThrow().status);
 }
 @Test void adminAccountDirectoryDoesNotReturnCredentialFields() throws Exception {
   AppUser admin=users.findByUsername("admin").orElseThrow();
   mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/accounts").header("Authorization","Bearer "+service.issue(admin)))
      .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
      .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].passwordHash").doesNotExist())
      .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].password").doesNotExist());
 }
 private long eventsFor(long id){return eventRepo.findByRequestIdOrderByCreatedAtAsc(id).size();}
}
