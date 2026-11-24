export type AppModule =
  | 'DASHBOARD' | 'STUDENT' | 'CENTER' | 'ROLE' | 'STAFF' | 'REPORTS' | 'REQUEST' | 'EXPENSE'
  | 'MESSAGING' | 'POSTS' | 'ASSESSMENT' | 'COACHING_DRILLS' | 'SETTINGS' | 'ATTENDANCE' | 'ATTENDANCE_APPROVAL';
export type AccessLevel = 'NONE' | 'VIEW' | 'MANAGE';
export type RoleType = 'HEAD_OFFICE' | 'CENTER' | 'STUDENT';
export type ActiveStatus = 'ACTIVE' | 'INACTIVE';
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type StudentStatus = 'ACTIVE' | 'SUBMITTED' | 'DEREGISTERED';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export const MODULES: { key: AppModule; label: string }[] = [
  { key: 'DASHBOARD', label: 'Dashboard' },
  { key: 'STUDENT', label: 'Student' },
  { key: 'ATTENDANCE', label: 'Attendance (mark)' },
  { key: 'ATTENDANCE_APPROVAL', label: 'Attendance (approve)' },
  { key: 'CENTER', label: 'Class' },
  { key: 'ROLE', label: 'Roles' },
  { key: 'STAFF', label: 'Staff' },
  { key: 'REPORTS', label: 'Reports' },
  { key: 'REQUEST', label: 'Request' },
  { key: 'EXPENSE', label: 'Expense' },
  { key: 'MESSAGING', label: 'Messaging' },
  { key: 'POSTS', label: 'Posts' },
  { key: 'ASSESSMENT', label: 'Assessment' },
  { key: 'COACHING_DRILLS', label: 'Coaching Drills' },
  { key: 'SETTINGS', label: 'Settings' },
];

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export interface Me {
  id: string;
  firstName: string;
  lastName?: string;
  email?: string;
  platformAdmin: boolean;
}

export interface TokenResponse {
  accessToken: string;
  expiresIn: number;
  user: Me;
}

export interface AccessibleOrganization {
  id: string;
  name: string;
  city?: string;
  country?: string;
  primary: boolean;
  affiliates?: AccessibleOrganization[];
}

export interface Organization {
  id: string;
  parentId?: string;
  name: string;
  city?: string;
  state?: string;
  country?: string;
  address?: string;
  email?: string;
  phone?: string;
  sport?: string;
  status: string;
  centerLabel: string;
  currency: string;
  dateFormat: string;
}

export interface MyAccess {
  organization: Organization;
  permissions: Partial<Record<AppModule, AccessLevel>>;
  centerId?: string;
  affiliateCount: number;
}

export interface RoleSummary {
  id: string;
  name: string;
  roleType: RoleType;
  systemRole: boolean;
}

export interface Role extends RoleSummary {
  permissions: Partial<Record<AppModule, AccessLevel>>;
}

export interface Staff {
  membershipId: string;
  userId: string;
  firstName: string;
  lastName?: string;
  email?: string;
  countryCode?: string;
  mobile?: string;
  roleId: string;
  roleName: string;
  roleType: RoleType;
  centerId?: string;
  status: 'ACTIVE' | 'DEREGISTERED';
  dateOfJoining?: string;
  qualifications?: string;
  profile?: string;
  passwordSet: boolean;
}

export interface Option {
  id: string;
  name: string;
  parentId?: string;
}

export interface CenterSummary {
  id: string;
  name: string;
  status: ActiveStatus;
  city?: string;
  country?: string;
  batches: number;
  students: number;
  capacity: number;
}

export interface Center {
  id: string;
  name: string;
  code?: string;
  contactName?: string;
  contactCountryCode?: string;
  contactPhone?: string;
  contactEmail?: string;
  sport?: string;
  facilityType?: string;
  facilityCount?: number;
  status: ActiveStatus;
  country?: string;
  state?: string;
  city?: string;
  address1?: string;
  address2?: string;
  zipcode?: string;
  latitude?: number;
  longitude?: number;
  geofenceRadiusM: number;
}

export interface Slot {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}

export interface Batch {
  id: string;
  centerId: string;
  name: string;
  startDate?: string;
  endDate?: string;
  slots: Slot[];
  coaches: { userId: string; name: string }[];
  capacity: number;
  status: ActiveStatus;
  description?: string;
  students: number;
}

export interface NamedRef {
  id: string;
  name: string;
}

export interface StudentSummary {
  id: string;
  studentNo: number;
  firstName: string;
  lastName?: string;
  center: NamedRef;
  batches: NamedRef[];
  age?: number;
  grade?: string;
  email?: string;
  status: StudentStatus;
}

export interface Student {
  id: string;
  studentNo: number;
  center: NamedRef;
  batches: NamedRef[];
  firstName: string;
  lastName?: string;
  email?: string;
  gender: Gender;
  dateOfBirth: string;
  age?: number;
  countryCode?: string;
  mobile?: string;
  uniqueId?: string;
  bloodGroup?: string;
  country?: string;
  state?: string;
  city?: string;
  address?: string;
  pinCode?: string;
  schoolName?: string;
  schoolAddress?: string;
  grade?: string;
  dominantSide?: 'LEFT' | 'RIGHT' | 'BOTH';
  medicalCondition?: string;
  status: StudentStatus;
}

export interface HeadcountRow {
  id: string;
  name: string;
  male: number;
  female: number;
  other: number;
  total: number;
}

export interface UtilizationRow {
  id: string;
  name: string;
  students: number;
  capacity: number;
  utilization: number;
}

export interface GeneralDashboard {
  totalStudents: number;
  totalCenters: number;
  totalBatches: number;
  totalCapacity: number;
  male: number;
  female: number;
  other: number;
  ageBuckets: Record<string, number>;
  groupedBy: 'ACADEMY' | 'CENTER';
  headcount: HeadcountRow[];
  utilization: UtilizationRow[];
}
