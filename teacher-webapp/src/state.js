import { useState } from 'react';

// Sample data for teachers and students
export const teachers = [
  { id: '917999435373', name: 'Kavyansh Chourasia', phone: '917999435373' },
  { id: '918962884701', name: 'Prajjwal Jha', phone: '918962884701' }
];

export const students = [
  { id: '917999710236', name: 'Ashwani', phone: '917999710236', raisedHand: false },
  { id: '918904954955', name: 'Feature Phone', phone: '918904954955', raisedHand: false }
];

export function useSelections() {
    const [selectedTeacher, setSelectedTeacher] = useState(null);
    const [selectedStudents, setSelectedStudents] = useState([]);
    const [userList, setUserList] = useState([]);
  
    const handleTeacherSelect = (teacher) => {
      if (selectedTeacher?.id === teacher.id) {
        setSelectedTeacher(null);
      } else {
        setSelectedTeacher(teacher);
      }
    };
  
    const handleStudentToggle = (student) => {
      if (selectedStudents.some((s) => s.id === student.id)) {
        setSelectedStudents(selectedStudents.filter((s) => s.id !== student.id));
      } else {
        setSelectedStudents([...selectedStudents, student]);
      }
    };
  
    const handleSubmit = () => {
      const allUsers = [
        { ...selectedTeacher, mute: false, status: 'DISCONNECTED', raisedHand: false, role: 'teacher' },
        ...selectedStudents.map((s) => ({
          ...s,
          mute: false,
          status: 'DISCONNECTED',
          role: 'student',
        })),
      ];
      setUserList(allUsers);
    };
  
    return {
      selectedTeacher,
      selectedStudents,
      userList,
      handleTeacherSelect,
      handleStudentToggle,
      handleSubmit,
    };
  }
  
