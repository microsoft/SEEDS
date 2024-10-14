import React, { useState } from 'react';
import './App.css';

export function DetailsPage({ userList, confId }) {
  const [users, setUsers] = useState(userList);
  const [loadingIds, setLoadingIds] = useState([]); // Track the loading state by user ID
  const [isCallStarted, setIsCallStarted] = useState(false); // Track call state
  const [isMusicPlaying, setIsMusicPlaying] = useState(false); // Track music state
  const [isLoadingCall, setIsLoadingCall] = useState(false); // Track loading state for call
  const [isLoadingMusic, setIsLoadingMusic] = useState(false); // Track loading state for music

  const teacher = users.find((user) => user.role === 'teacher');
  const students = users.filter((user) => user.role === 'student');

  const handleMuteToggle = (userToUpdate) => {
    // Show loading indicator for this user
    setLoadingIds((prev) => [...prev, userToUpdate.id]);

    // Simulate a 2-second delay for loading
    setTimeout(() => {
      // Update the user's mute state
      const updatedUsers = users.map((user) =>
        user.id === userToUpdate.id ? { ...user, mute: !user.mute } : user
      );
      setUsers(updatedUsers);

      // Remove the loading state for this user
      setLoadingIds((prev) => prev.filter((id) => id !== userToUpdate.id));
    }, 2000);
  };

  const handleStartCall = async () => {
    setIsLoadingCall(true); // Start loading
    const api_base = process.env.REACT_APP_CONF_SERVER_BASE_URI  + '/conference';
    const response = await fetch(api_base + `/start/${confId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (response.ok){
      setIsCallStarted((prev) => !prev); // Toggle call state
    }
    setIsLoadingCall(false);
  };

  const handlePlayMusic = () => {
    setIsLoadingMusic(true); // Start loading

    setTimeout(() => {
      setIsMusicPlaying((prev) => !prev); // Toggle music state
      setIsLoadingMusic(false); // Stop loading
    }, 2000);
  };

  

  const isLoading = (id) => loadingIds.includes(id);

  return (
    <div className="app-container">
      <h1 className="welcome-title">Details</h1>
      <div className="list-container">
        {/* Teacher Section */}
        {teacher && (
          <div className="list-box">
            <h2 className="list-title">Teacher</h2>
            <ul className="list">
              <li key={teacher.id} className="list-item">
                <div className="list-item-content">
                  <span className="content"><strong>{teacher.name}</strong></span>
                </div>
                <div className="list-item-content">
                  <span className="content"><strong>{teacher.phone}</strong></span>
                </div>
                <div className="list-item-content">
                  <span className="content"><strong>{teacher.status}</strong></span>
                </div>
                <div className="list-item-content">
                  <span className="content">
                    <button
                      onClick={() => handleMuteToggle(teacher)}
                      disabled={isLoading(teacher.id)} // Disable button while loading
                      className="mute-button"
                    >
                      {isLoading(teacher.id) ? 'Loading...' : teacher.mute ? 'Unmute' : 'Mute'}
                    </button>
                  </span>
                </div>
                <div className="list-item-content">
                  {teacher.raisedHand && (
                    <span className="content raised-hand">✋</span>
                  )}
                </div>
              </li>
            </ul>
          </div>
        )}

        {/* Students Section */}
        {students.length > 0 && (
          <div className="list-box">
            <h2 className="list-title">Students</h2>
            <ul className="list">
              {students.map((student) => (
                <li key={student.id} className="list-item">
                  <div className="list-item-content">
                    <span className="content"><strong>{student.name}</strong></span>
                  </div>
                  <div className="list-item-content">
                    <span className="content"><strong>{student.phone}</strong></span>
                  </div>
                  <div className="list-item-content">
                    <span className="content"><strong>{student.status}</strong></span>
                  </div>
                  <div className="list-item-content">
                    <span className="content">
                      <button
                        onClick={() => handleMuteToggle(student)}
                        disabled={isLoading(student.id)} // Disable button while loading
                        className="mute-button"
                      >
                        {isLoading(student.id) ? 'Loading...' : student.mute ? 'Unmute' : 'Mute'}
                      </button>
                    </span>
                  </div>
                  <div className="list-item-content">
                    {student.raisedHand && (
                      <span className="content raised-hand">✋</span>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {/* Buttons below the container */}
      <div className="button-container">
        <button
          className="action-button"
          onClick={handleStartCall}
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
