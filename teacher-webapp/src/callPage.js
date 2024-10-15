import React, { useState, useEffect } from 'react';
import { useConference } from './context/ConferenceContext';
import { startConferenceCall, endConferenceCall, muteParticipant, unmuteParticipant } from './services/apiService';
import './App.css';

export function DetailsPage() {
  const {
    userList,
    confId
  } = useConference();

  const [users, setUsers] = useState(userList);
  const [loadingIds, setLoadingIds] = useState([]);
  const [isCallStarted, setIsCallStarted] = useState(false);
  const [isMusicPlaying, setIsMusicPlaying] = useState(false);
  const [isLoadingCall, setIsLoadingCall] = useState(false);
  const [isLoadingMusic, setIsLoadingMusic] = useState(false);

  useEffect(() => {
    setUsers(userList);
  }, [userList]);

  const teacher = users.find((user) => user.role === 'Teacher');
  const students = users.filter((user) => user.role === 'Student');

  const handleMuteToggle = async (userToUpdate) => {
    setLoadingIds((prev) => [...prev, userToUpdate.phone_number]);

    if (userToUpdate.is_muted){
      await unmuteParticipant(confId, userToUpdate.phone_number)
    }else{
      await muteParticipant(confId, userToUpdate.phone_number)
    }
    
    setLoadingIds((prev) => prev.filter((id) => id !== userToUpdate.phone_number));
  };

  const handleStartCall = async () => {
    setIsLoadingCall(true);
    try {
      const response = await startConferenceCall(confId);
      if (response.ok) {
        setIsCallStarted((prev) => !prev);
      }
    } catch (error) {
      console.error('Error starting the call:', error);
    } finally {
      setIsLoadingCall(false);
    }
  };

  const handleEndCall = async () => {
    setIsLoadingCall(true);
    try {
      const response = await endConferenceCall(confId);
      if (response.ok) {
        setIsCallStarted((prev) => !prev);
      }
    } catch (error) {
      console.error('Error starting the call:', error);
    } finally {
      setIsLoadingCall(false);
    }
  }

  const handlePlayMusic = () => {
    setIsLoadingMusic(true);
    setTimeout(() => {
      setIsMusicPlaying((prev) => !prev);
      setIsLoadingMusic(false);
    }, 2000);
  };

  const isLoading = (id) => loadingIds.includes(id);

  return (
    <div className="app-container">
      <h1 className="welcome-title">Details</h1>
      <div className="list-container">
        {teacher && (
          <div className="list-box">
            <h2 className="list-title">Teacher</h2>
            <ul className="list">
              <li key={teacher.phone_number} className="list-item">
                <div className="list-item-content">
                  <span className="content"><strong>{teacher.name}</strong></span>
                </div>
                <div className="list-item-content">
                  <span className="content"><strong>{teacher.phone_number}</strong></span>
                </div>
                <div className="list-item-content">
                  <span className="content"><strong>{teacher.call_status}</strong></span>
                </div>
                <div className="list-item-content">
                  <span className="content">
                    <button
                      onClick={() => handleMuteToggle(teacher)}
                      disabled={isLoading(teacher.phone_number) || teacher.call_status !== "connected"}
                      className="mute-button"
                    >
                      {isLoading(teacher.phone_number) ? 'Loading...' : teacher.is_muted ? 'Unmute' : 'Mute'}
                    </button>
                  </span>
                </div>
              </li>
            </ul>
          </div>
        )}

        {students.length > 0 && (
          <div className="list-box">
            <h2 className="list-title">Students</h2>
            <ul className="list">
              {students.map((student) => (
                <li key={student.phone_number} className="list-item">
                  <div className="list-item-content">
                    <span className="content"><strong>{student.name}</strong></span>
                  </div>
                  <div className="list-item-content">
                    <span className="content"><strong>{student.phone_number}</strong></span>
                  </div>
                  <div className="list-item-content">
                    <span className="content"><strong>{student.call_status}</strong></span>
                  </div>
                  <div className="list-item-content">
                    <span className="content">
                      <button
                        onClick={() => handleMuteToggle(student)}
                        disabled={isLoading(student.phone_number) || student.call_status !== "connected"}
                        className="mute-button"
                      >
                        {isLoading(student.phone_number) ? 'Loading...' : student.is_muted ? 'Unmute' : 'Mute'}
                      </button>
                    </span>
                  </div>
                  {student.is_raised && (
                    <div className="list-item-content">
                      <span className="raised-hand-icon" role="img" aria-label="raised hand">✋</span>
                    </div>
                  )
                  }
                  
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      <div className="button-container">
        <button
          className="action-button"
          onClick={isCallStarted ? handleEndCall : handleStartCall}
          disabled={isLoadingCall}
        >
          {isLoadingCall ? 'Loading...' : isCallStarted ? 'End Call' : 'Start Call'}
        </button>
        <button className="action-button">Add Participant</button>
        <button
          className="action-button"
          onClick={handlePlayMusic}
          disabled={isLoadingMusic}
        >
          {isLoadingMusic ? 'Loading...' : isMusicPlaying ? 'Pause Music' : 'Play Music'}
        </button>
      </div>
    </div>
  );
}
