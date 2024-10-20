import React, { useState, useEffect } from 'react';
import { useConference } from './context/ConferenceContext';
import { startConferenceCall, endConferenceCall, sinkConferenceCall, muteParticipant, unmuteParticipant, playAudio, pauseAudio, addParticipant } from './services/apiService';
import { AddParticipantModal } from './components/AddParticipantModal';
import { students as allStudents } from './state';
import App from './App';

export function DetailsPage() {
  const {
    userList,
    confId,
    isConfCallRunning,
    audioContentState,
  } = useConference();

  const [users, setUsers] = useState(userList);
  const [loadingIds, setLoadingIds] = useState([]);
  const [reconnectingIds, setReconnectingIds] = useState([]);
  const [isLoadingCall, setIsLoadingCall] = useState(false);
  const [isSinkingConf, setIsSinkingConf] = useState(false)
  const [hasSunkConf, setHasSunkConf] = useState(false)
  const [isLoadingMusic, setIsLoadingMusic] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    setUsers(userList);
  }, [userList]);

  const teacher = users.find((user) => user.role === 'Teacher');
  const students = users.filter((user) => user.role === 'Student');

  const handleMuteToggle = async (userToUpdate) => {
    setLoadingIds((prev) => [...prev, userToUpdate.phone_number]);

    if (userToUpdate.is_muted) {
      await unmuteParticipant(confId, userToUpdate.phone_number)
    } else {
      await muteParticipant(confId, userToUpdate.phone_number)
    }

    setLoadingIds((prev) => prev.filter((id) => id !== userToUpdate.phone_number));
  };

  const handleStartCall = async () => {
    setIsLoadingCall(true);
    try {
      await startConferenceCall(confId);
    } catch (error) {
      console.error('Error starting the call:', error);
    } finally {
      setIsLoadingCall(false);
    }
  };

  const handleEndCall = async () => {
    setIsLoadingCall(true);
    try {
      await endConferenceCall(confId);
    } catch (error) {
      console.error('Error starting the call:', error);
    } finally {
      setIsLoadingCall(false);
    }
  }

  const handleSinkConf = async () => {
    setIsSinkingConf(true);
    try {
      await sinkConferenceCall(confId);
    } catch (error) {
      console.error('Error starting the call:', error);
    } finally {
      setIsSinkingConf(false);
      setHasSunkConf(true)
    }
  }

  const handlePlayMusic = async () => {
    setIsLoadingMusic(true);
    if (audioContentState.status === "Playing") {
      await pauseAudio(confId)
    } else {
      await playAudio(confId)
    }
    setIsLoadingMusic(false);
  };

  const handleReconnect = async (phone_number) => {
    setReconnectingIds((prev) => [...prev, phone_number]);

    await addParticipant(confId, phone_number)

    setReconnectingIds((prev) => prev.filter((id) => id !== phone_number));
  }

  const handleOpenModal = () => setIsModalOpen(true);
  const handleCloseModal = () => setIsModalOpen(false);

  const handleAddParticipants = async (selectedPhoneNumbers) => {
    for (const phone_number of selectedPhoneNumbers) {
      await addParticipant(confId, phone_number);
    }
  };

  // Filter out students who are already in the userList
  const availableStudents = allStudents.filter(
    (student) => !userList.some((user) => user.phone_number === student.phone_number)
  );

  const isLoading = (phone_number) => loadingIds.includes(phone_number);
  const isPlayingAudio = audioContentState.status === "Playing"
  const isStartingAudio = audioContentState.status === "Starting"

  const canReconnect = (user) => user.call_status === "disconnected" && isConfCallRunning
  const isReconnecting = (phone_number) => reconnectingIds.includes(phone_number)

  if(hasSunkConf){
    return <App />
  }

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
                {canReconnect(teacher) && (
                  <div className="list-item-content">
                    <span className="content">
                      <button
                        onClick={() => handleReconnect(teacher.phone_number)}
                        className="mute-button"
                      >
                        {isReconnecting(teacher.phone_number) ? 'Loading...' : 'Reconnect'}
                      </button>
                    </span>
                  </div>
                )}
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
                  {canReconnect(student) && (
                    <div className="list-item-content">
                      <span className="content">
                        <button
                          onClick={() => handleReconnect(student.phone_number)}
                          className="mute-button"
                        >
                          {isReconnecting(student.phone_number) ? 'Loading...' : 'Reconnect'}
                        </button>
                      </span>
                    </div>
                  )}
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
          onClick={isConfCallRunning ? handleEndCall : handleStartCall}
          disabled={isLoadingCall}
        >
          {isLoadingCall ? 'Loading...' : isConfCallRunning ? 'End Call' : 'Start Call'}
        </button>

        <button
          className="action-button"
          onClick={handleSinkConf}
          disabled={isConfCallRunning || isSinkingConf}
        >
          {isSinkingConf ? 'Sinking...' : 'Sink Conference'}
        </button>

        <button className="action-button"
          onClick={handleOpenModal}
          disabled={!isConfCallRunning}
        >
          Add Participant
        </button>
        <button
          className="action-button"
          onClick={handlePlayMusic}
          disabled={isLoadingMusic || !isConfCallRunning || isStartingAudio}
        >
          {isLoadingMusic ? 'Loading...' : isStartingAudio? "Starting..." :  isPlayingAudio ? 'Pause Music' : 'Play Music'}
        </button>
      </div>
      <AddParticipantModal
        open={isModalOpen}
        onClose={handleCloseModal}
        availableStudents={availableStudents}
        onSubmit={handleAddParticipants}
      />
    </div>
  );
}
