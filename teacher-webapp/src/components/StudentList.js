import React from 'react';

export const StudentList = ({ students, selectedStudents, handleStudentToggle }) => (
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
);
