import React from 'react';
import { useConference } from './context/ConferenceContext';
import { DetailsPage } from './callPage';
import { TeacherList } from './components/TeacherList';
import { StudentList } from './components/StudentList';
import { createConference } from './services/apiService';
import { teachers, students } from './state'; // Import teachers and students
import './App.css';

const getCurrentTime = () => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0'); // Months are zero-based
  const day = String(now.getDate()).padStart(2, '0');
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  const seconds = String(now.getSeconds()).padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

function App() {
  const {
    selectedTeacher,
    selectedStudents,
    setConfId,
    loading,
    setLoading,
    handleSSEEvent,
    handleTeacherSelect,
    handleStudentToggle,
  } = useConference();

  const [isSubmitted, setIsSubmitted] = React.useState(false);

  const handleFormSubmit = async () => {
    setLoading(true); // Start loading
    try {
      const data = await createConference(
        selectedTeacher.phone_number,
        selectedStudents.map((item) => item.phone_number)
      );
      const conferenceId = data.id;
      setConfId(conferenceId);
      console.log('Conf ID:', conferenceId);

      const sseEp = `${process.env.REACT_APP_CONF_SERVER_BASE_URI}/conference/teacherappconnect/${conferenceId}`;
      const eventSource = new EventSource(sseEp);

      eventSource.onmessage = (event) => {
        console.log(`${getCurrentTime()} Message from SSE:`, event.data);
        const parsedData = JSON.parse(event.data);
        handleSSEEvent(parsedData);
      };

      eventSource.onerror = (err) => {
        console.error("SSE Error:", err);
        eventSource.close();
      };

      setIsSubmitted(true); // Navigate to DetailsPage
    } catch (error) {
      console.error('Error in API call:', error);
    } finally {
      setLoading(false); // Stop loading
    }
  };

  if (isSubmitted) {
    return <DetailsPage />;
  }

  return (
    <div className="app-container">
      <h1 className="welcome-title">Welcome</h1>
      <div className="list-container">
        <TeacherList
          teachers={teachers}
          selectedTeacher={selectedTeacher}
          handleTeacherSelect={handleTeacherSelect}
        />
        <StudentList
          students={students}
          selectedStudents={selectedStudents}
          handleStudentToggle={handleStudentToggle}
        />
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
