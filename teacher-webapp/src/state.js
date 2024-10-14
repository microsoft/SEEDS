import { useState, useEffect } from 'react';

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



// Sample data for teachers and students
export const teachers = [
  new Participant({name:'Kavyansh Chourasia', phone_number:'917999435373', role:'Teacher'}),
  new Participant({name:'Prajjwal Jha', phone_number:'918962884701', role:'Teacher'})
];

export const students = [
  new Participant({name:'Ashwani', phone_number:'917999710236', role:'Student'}),
  new Participant({name:'Feature Phone', phone_number:'918904954955', role:'Student'})
];

export function useSelections() {
  const [selectedTeacher, setSelectedTeacher] = useState(null);
  const [selectedStudents, setSelectedStudents] = useState([]);
  const [userList, setUserList] = useState([]);

  const handleSSEEvent = (event) => {
    for (let phone_number in event.participants) {
      const participant = new Participant({ ...event.participants[phone_number] });
  
      if (selectedTeacher?.phone_number === phone_number) {
        // Update the selected teacher
        const newTeacher = new Participant({
          ...selectedTeacher,
          raised_at: participant.raised_at,
          is_raised: participant.is_raised,
          is_muted: participant.is_muted,
          call_status: participant.call_status
        });
        // console.log('UPDATED TEACHER')
        // console.log(newTeacher)
        setSelectedTeacher(newTeacher);  // Properly update teacher state
      } else {
        // Update the selected students
        const newStudents = selectedStudents.map((student) => {
          if (student.phone_number === phone_number) {
            return new Participant({
              ...student,
              raised_at: participant.raised_at,
              is_raised: participant.is_raised,
              is_muted: participant.is_muted,
              call_status: participant.call_status
            });
          }
          return new Participant({...student});
        });
        setSelectedStudents(newStudents);  // Properly update students state
        // console.log("UPDATED STUDENTS")
        // console.log(newStudents)
      }
    }
  };

  const handleTeacherSelect = (teacher) => {
    if (selectedTeacher?.phone_number === teacher.phone_number) {
      setSelectedTeacher(null);
    } else {
      setSelectedTeacher(teacher);
    }
  };

  const handleStudentToggle = (student) => {
    if (selectedStudents.some((s) => s.phone_number === student.phone_number)) {
      setSelectedStudents(selectedStudents.filter((s) => s.phone_number !== student.phone_number));
    } else {
      setSelectedStudents([...selectedStudents, student]);
    }
  };

  useEffect(() => {
    const allUsers = [
      selectedTeacher, // Add selected teacher
      ...selectedStudents, // Spread the selected students
    ].filter(Boolean); // Filter out null or undefined values
    setUserList(allUsers);
    // console.log("UPDATING ALL USERS")
  }, [selectedTeacher, selectedStudents]); 

  return {
    selectedTeacher,
    selectedStudents,
    userList,
    handleSSEEvent,
    handleTeacherSelect,
    handleStudentToggle
  };
}
