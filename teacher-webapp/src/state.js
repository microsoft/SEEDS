// Define a Participant class
export class Participant {
  constructor({
    name = "Unknown",
    phone_number = "0000000000",
    role = "Student",
    raised_at = -1,
    is_raised = false,
    is_muted = true,
    call_status = "disconnected"
  } = {}) { // Destructure object and provide default values
    this.name = name;
    this.phone_number = phone_number;
    this.role = role;
    this.raised_at = raised_at;
    this.is_raised = is_raised;
    this.is_muted = is_muted;
    this.call_status = call_status;
  }
}

export class AudioContentState {
  constructor({
    current_url = "", 
    status = "Paused", 
    paused_at = ""
  } = {}) {
    this.current_url = current_url;
    this.status = status;
    this.paused_at = paused_at;
  }
}

// Sample data for teachers and students
export const teachers = [
  new Participant({ name: 'Kavyansh Chourasia', phone_number: '917999435373', role: 'Teacher' }),
  new Participant({ name: 'Prajjwal Jha', phone_number: '918962884701', role: 'Teacher' })
];

export const students = [
  new Participant({ name: 'Smart Phone Motorola', phone_number: '918904954836', role: 'Student' }),
  new Participant({ name: 'Ashwani', phone_number: '917999710236', role: 'Student' }),
  new Participant({ name: 'Feature Phone', phone_number: '918904954955', role: 'Student' })
];
