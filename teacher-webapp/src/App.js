import React from 'react';
import { useSelections, teachers, students } from './state';
import { DetailsPage } from './callPage';
import './App.css';

function App() {
  const {
    selectedTeacher,
    selectedStudents,
    userList,
    handleSSEEvent,
    handleTeacherSelect,
    handleStudentToggle
  } = useSelections();

  const [isSubmitted, setIsSubmitted] = React.useState(false);
  const [loading, setLoading] = React.useState(false); // To track loading state
  const [confId, setconfId] = React.useState('');

  const handleFormSubmit = async () => {
    setLoading(true); // Start loading
    const api_base = process.env.REACT_APP_CONF_SERVER_BASE_URI  + '/conference';
    // console.log(api_base)
    try {
      // First API call
      const response1 = await fetch(api_base + '/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          "teacher_phone": selectedTeacher.phone_number,
          "student_phones": selectedStudents.map((item) => item.phone_number),
        }),
      });
      
      // Check if the response status is OK (status code 2xx)
      if (response1.ok) {
        const data = await response1.json();
        const conferenceId = data.id
        setconfId(conferenceId)
        console.log('Conf ID:', conferenceId); 

        const sseEp = `${api_base}/teacherappconnect/${conferenceId}`
        // console.log(sseEp)
        console.log("CALLING SSE")
        // Connect to SSE endpoint using the conference ID from the first API call
        const eventSource = new EventSource(sseEp);

        // Log incoming messages from SSE
        eventSource.onmessage = (event) => {
          console.log("Message from SSE:", event.data);
          const parsedData = JSON.parse(event.data);
          handleSSEEvent(parsedData)
        };

        eventSource.onerror = (err) => {
          console.error("SSE Error:", err);
          eventSource.close(); 
        };
      } else {
        console.error('Failed with status code:', response1.status);
        // You can throw an error or handle it according to your needs
        const errorMessage = await response1.text(); // Get the error message (if any)
        throw new Error(`Error ${response1.status}: ${errorMessage}`);
      }
      setIsSubmitted(true); // Navigate to DetailsPage
    } catch (error) {
      console.error('Error in API calls:', error);
    } finally {
      setLoading(false); // Stop loading
    }
  };

  if (isSubmitted) {
    return <DetailsPage userList={userList} confId={confId} />;
  }

  return (
    <div className="app-container">
      <h1 className="welcome-title">Welcome</h1>
      <div className="list-container">
        <div className="list-box">
          <h2 className="list-title">Teacher</h2>
          <ul className="list">
            {teachers.map((teacher) => (
              <li
                key={teacher.phone_number}
                className={`list-item ${selectedTeacher?.phone_number === teacher.phone_number ? 'selected' : ''}`}
                onClick={() => handleTeacherSelect(teacher)}
              >
                <div className="list-item-content">
                  <span>{teacher.name} - {teacher.phone_number}</span>
                </div>
              </li>
            ))}
          </ul>
        </div>
        <div className="list-box">
          <h2 className="list-title">Students</h2>
          <ul className="list">
            {students.map((student) => (
              <li
                key={student.phone_number}
                className={`list-item ${selectedStudents.some((s) => s.phone_number === student.phone_number) ? 'selected' : ''}`}
                onClick={() => handleStudentToggle(student)}
              >
                <div className="list-item-content">
                  <span>{student.name} - {student.phone_number}</span>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>
      <button
        className="submit-button"
        onClick={handleFormSubmit}
        disabled={!selectedTeacher || selectedStudents.length === 0 || loading}
      >
        {loading ? 'Submitting...' : 'Submit'}
      </button>
    </div>
  );
}

export default App;
