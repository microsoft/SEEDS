import React, { useState } from 'react';

export const AddParticipantModal = ({ open, onClose, availableStudents, onSubmit }) => {
    const [selectedStudents, setSelectedStudents] = useState([]);

    if (!open) return null; // If modal is not open, don't render anything

    const handleToggleStudent = (phone_number) => {
        setSelectedStudents((prevSelected) =>
            prevSelected.includes(phone_number)
                ? prevSelected.filter((id) => id !== phone_number)
                : [...prevSelected, phone_number]
        );
    };

    const handleSubmit = () => {
        onSubmit(selectedStudents);
        setSelectedStudents([]);
        onClose();
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <h2>Select Participants to Add</h2>
                <ul className="student-list">
                    {availableStudents.map((student) => (
                        <li key={student.phone_number}>
                            <label>
                                <input
                                    type="checkbox"
                                    checked={selectedStudents.includes(student.phone_number)}
                                    onChange={() => handleToggleStudent(student.phone_number)}
                                />
                                {student.name} - {student.phone_number}
                            </label>
                        </li>
                    ))}
                </ul>
                <div className="modal-actions">
                    <button onClick={handleSubmit} disabled={selectedStudents.length === 0}>
                        Submit
                    </button>
                    <button onClick={onClose}>Cancel</button>
                </div>
            </div>
        </div>
    );
};
