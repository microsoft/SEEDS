import React, { createContext, useContext, useState, useEffect } from 'react';
import { AudioContentState, Participant } from '../state'; // You can import from existing state file

const ConferenceContext = createContext();

export const useConference = () => useContext(ConferenceContext);

export const ConferenceProvider = ({ children }) => {
  const [isConfCallRunning, setIsConfCallRunning] = useState(false)
  const [audioContentState, setAudioContentState] = useState(new AudioContentState())
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
    setIsConfCallRunning(event.is_running);
    setAudioContentState(new AudioContentState(event.audio_content_state));

    for (let phone_number in event.participants) {
      const participant = new Participant({ ...event.participants[phone_number] });

      if (selectedTeacher?.phone_number === phone_number) {
        // Update the teacher if the phone_number matches the selected teacher
        const newTeacher = new Participant({
          ...selectedTeacher,
          raised_at: participant.raised_at,
          is_raised: participant.is_raised,
          is_muted: participant.is_muted,
          call_status: participant.call_status,
        });
        setSelectedTeacher(newTeacher);
      } else {
        // Check if the phone_number matches any of the existing students
        const studentExists = selectedStudents.some(
          (student) => student.phone_number === phone_number
        );

        if (studentExists) {
          // Update the existing student if found
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
        } else {
          // If the phone_number is not found in the selected students, add it as a new student
          setSelectedStudents((prevStudents) => [
            ...prevStudents,
            participant,
          ]);
        }
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
        isConfCallRunning,
        audioContentState,
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
