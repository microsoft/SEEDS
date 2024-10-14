import React, { useState, useEffect } from 'react';
import './App.css';

export function DetailsPage({ userList, confId }) {
  const [users, setUsers] = useState(userList); // Local state for users
  const [loadingIds, setLoadingIds] = useState([]); // Track the loading state by user ID
  const [isCallStarted, setIsCallStarted] = useState(false); // Track call state
  const [isMusicPlaying, setIsMusicPlaying] = useState(false); // Track music state
  const [isLoadingCall, setIsLoadingCall] = useState(false); // Track loading state for call
  const [isLoadingMusic, setIsLoadingMusic] = useState(false); // Track loading state for music

  // Update local state whenever userList prop changes
  useEffect(() => {
    // console.log("Updating users from userList", userList);
    setUsers(userList);
  }, [userList]); 

  const teacher = users.find((user) => user.role === 'Teacher');
  const students = users.filter((user) => user.role === 'Student');

  const handleMuteToggle = (userToUpdate) => {
    // Show loading indicator for this user
    setLoadingIds((prev) => [...prev, userToUpdate.phone_number]);

    // Simulate a 2-second delay for loading
    setTimeout(() => {
      // Update the user's mute state
      const updatedUsers = users.map((user) =>
        user.phone_number === userToUpdate.phone_number ? { ...user, is_muted: !user.is_muted } : user
      );
      setUsers(updatedUsers);

      // Remove the loading state for this user
      setLoadingIds((prev) => prev.filter((id) => id !== userToUpdate.phone_number));
    }, 2000);
  };

  const handleStartCall = async () => {
    setIsLoadingCall(true); // Start loading
    const api_base = process.env.REACT_APP_CONF_SERVER_BASE_URI + '/conference';
    const response = await fetch(api_base + `/start/${confId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (response.ok) {
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
                      disabled={isLoading(teacher.phone_number)} // Disable button while loading
                      className="mute-button"
                    >
                      {isLoading(teacher.phone_number) ? 'Loading...' : teacher.is_muted ? 'Unmute' : 'Mute'}
                    </button>
                  </span>
                </div>
                <div className="list-item-content">
                  {teacher.is_raised && (
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
                        disabled={isLoading(student.phone_number)} // Disable button while loading
                        className="mute-button"
                      >
                        {isLoading(student.phone_number) ? 'Loading...' : student.is_muted ? 'Unmute' : 'Mute'}
                      </button>
                    </span>
                  </div>
                  <div className="list-item-content">
                    {student.is_raised && (
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
