import React from 'react';

export const TeacherList = ({ teachers, selectedTeacher, handleTeacherSelect }) => (
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
);
