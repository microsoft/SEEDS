import React, { createContext, useContext, useState, useEffect } from 'react';
import { Participant } from '../state'; // You can import from existing state file

const ConferenceContext = createContext();

export const useConference = () => useContext(ConferenceContext);

export const ConferenceProvider = ({ children }) => {
  const [selectedTeacher, setSelectedTeacher] = useState(null);
  const [selectedStudents, setSelectedStudents] = useState([]);
  const [userList, setUserList] = useState([]);
  const [confId, setConfId] = useState('');
  const [loading, setLoading] = useState(false);

  // Updates the `userList` whenever teacher or students are selected
  useEffect(() => {
    const allUsers = [selectedTeacher, ...selectedStudents].filter(Boolean); // Filter out null values
    setUserList(allUsers);
  }, [selectedTeacher, selectedStudents]);

  const handleTeacherSelect = (teacher) => {
    setSelectedTeacher((prev) =>
      prev?.phone_number === teacher.phone_number ? null : teacher
    );
  };

  const handleStudentToggle = (student) => {
    setSelectedStudents((prevStudents) =>
      prevStudents.some((s) => s.phone_number === student.phone_number)
        ? prevStudents.filter((s) => s.phone_number !== student.phone_number)
        : [...prevStudents, student]
    );
  };

  const handleSSEEvent = (event) => {
    // Updating participants (teacher or students) from the SSE event data
    for (let phone_number in event.participants) {
      const participant = new Participant({ ...event.participants[phone_number] });

      if (selectedTeacher?.phone_number === phone_number) {
        const newTeacher = new Participant({
          ...selectedTeacher,
          raised_at: participant.raised_at,
          is_raised: participant.is_raised,
          is_muted: participant.is_muted,
          call_status: participant.call_status,
        });
        setSelectedTeacher(newTeacher);
      } else {
        const newStudents = selectedStudents.map((student) =>
          student.phone_number === phone_number
            ? new Participant({
                ...student,
                raised_at: participant.raised_at,
                is_raised: participant.is_raised,
                is_muted: participant.is_muted,
                call_status: participant.call_status,
              })
            : student
        );
        setSelectedStudents(newStudents);
      }
    }
  };

  return (
    <ConferenceContext.Provider
      value={{
        selectedTeacher,
        selectedStudents,
        userList,
        confId,
        setConfId,
        loading,
        setLoading,
        handleSSEEvent,
        handleTeacherSelect,
        handleStudentToggle,
      }}
    >
      {children}
    </ConferenceContext.Provider>
  );
};
