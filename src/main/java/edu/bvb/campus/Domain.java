package edu.bvb.campus;

import jakarta.persistence.*;
import java.time.Instant;

enum Role { ADMIN, REQUESTER, TECHNICIAN }
enum Priority { HIGH, MEDIUM, LOW }
enum RequestStatus { QUEUED, SUBMITTED, ASSIGNED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }

@Entity @Table(name="app_user")
class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false,unique=true) String username;
 @Column(nullable=false) String displayName;
 @Column(nullable=false) String passwordHash;
 @Enumerated(EnumType.STRING) @Column(nullable=false) Role role;
 boolean enabled=true;
 protected AppUser(){}
 AppUser(String u,String n,String h,Role r){username=u;displayName=n;passwordHash=h;role=r;}
}

@Entity @Table(name="technician_profile")
class TechnicianProfile {
 @Id Long userId;
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="technician_skills",joinColumns=@JoinColumn(name="technician_id")) @Column(name="skill") java.util.Set<String> skills=new java.util.HashSet<>();
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="technician_locations",joinColumns=@JoinColumn(name="technician_id")) @Column(name="location") java.util.Set<String> locations=new java.util.HashSet<>();
 boolean available=true;
 int maxActive=1;
 protected TechnicianProfile(){}
 TechnicianProfile(long id,String skills,String locations){userId=id;this.skills.addAll(java.util.Arrays.asList(skills.split(",")));this.locations.addAll(java.util.Arrays.asList(locations.split(",")));}
}

@Entity @Table(name="service_category")
class ServiceCategory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false,unique=true) String name;
 boolean active=true;
 protected ServiceCategory(){}
 ServiceCategory(String n){name=n;}
}
@Entity @Table(name="service_subcategory")
class ServiceSubcategory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(nullable=false) String name;
 @ManyToOne(optional=false) ServiceCategory category;
 @Column(nullable=false) String skill;
 @Enumerated(EnumType.STRING) Priority defaultPriority=Priority.MEDIUM;
 boolean active=true;
 protected ServiceSubcategory(){}
 ServiceSubcategory(String n,ServiceCategory c,String s,Priority p){name=n;category=c;skill=s;defaultPriority=p;}
}

@Entity @Table(name="service_request",indexes=@Index(name="idx_request_queue",columnList="status,priority,created_at"))
class ServiceRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @ManyToOne(optional=false) AppUser requester;
 @ManyToOne(optional=false) ServiceCategory category;
 @ManyToOne(optional=false) ServiceSubcategory subcategory;
 @Column(nullable=false) String title;
 @Column(nullable=false,length=2000) String description;
 @Column(nullable=false) String location;
 @Enumerated(EnumType.STRING) @Column(nullable=false) Priority priority;
 @Enumerated(EnumType.STRING) @Column(nullable=false) RequestStatus status;
 @Column(nullable=false,updatable=false) Instant createdAt=Instant.now();
 protected ServiceRequest(){}
}
@Entity @Table(name="service_assignment", uniqueConstraints=@UniqueConstraint(name="uq_active_assignment_request",columnNames={"request_id"}))
class ServiceAssignment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @OneToOne(optional=false) @JoinColumn(name="request_id",unique=true) ServiceRequest request;
 @ManyToOne(optional=false) AppUser technician;
 @Column(nullable=false) Instant assignedAt=Instant.now();
 protected ServiceAssignment(){}
}
@Entity @Table(name="status_event")
class StatusEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @ManyToOne(optional=false) ServiceRequest request;
 @ManyToOne(optional=false) AppUser actor;
 @Enumerated(EnumType.STRING) RequestStatus fromStatus;
 @Enumerated(EnumType.STRING) RequestStatus toStatus;
 @Column(length=500) String note;
 @Column(nullable=false,updatable=false) Instant createdAt=Instant.now();
 protected StatusEvent(){}
 StatusEvent(ServiceRequest r,AppUser a,RequestStatus from,RequestStatus to,String n){request=r;actor=a;fromStatus=from;toStatus=to;note=n;}
}
