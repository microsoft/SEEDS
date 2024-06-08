import React from "react";
import Content from "./Content";
import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import Multiselect from "multiselect-react-dropdown";
import { SEEDS_URL } from "../Constants";
import { useLocation } from 'react-router-dom';
import LogoutButton from "./LogoutButton";
import { getAuth, onAuthStateChanged } from 'firebase/auth';


const AllContent = () => {
  const [content, setContent] = useState([]);
  const [allContent, setAllContent] = useState([]);
  const [options, setOptions] = useState([]);
  const [updateIVRStatus, setUpdateIVRStatus] = useState('');
  const navigate = useNavigate();

  const [currentUser, setCurrentUser] = useState("");

  useEffect(() => {
    const auth = getAuth();
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setCurrentUser(user.displayName);
    });

    return () => unsubscribe(); // Clean up the listener when the component unmounts
  }, []);

  const onUpdateIVR = async () => {
    try {
      const response = await fetch(`https://ivrv2.azurewebsites.net/updateivr`, {
        method: "POST",
        headers: {
          authToken: "postman",
        },
      });
      const data = await response.json();
      setUpdateIVRStatus(data.message);
      console.log(data);
      // if (data.status_code == 200) {
      //   // throw new Error(data.message || "Failed to update IVR");
      // } 
      // setUpdateStatus(`Success: ${data.message}`);
    } catch (error) {
      // setUpdateStatus(`Error: ${error.message}`);
    }
  };

  const sortContentByCreationTime = (contentArray) => {
    return contentArray.sort((a, b) => b.creation_time - a.creation_time);
  };

  const generateOptions = (contentList) => {
    const languageSet = new Set();
    const experienceSet = new Set();

    contentList.forEach((contentItem) => {
      if (contentItem.language) {
        languageSet.add(contentItem.language.charAt(0).toUpperCase() + contentItem.language.slice(1)); // Capitalize the first letter
      }
      if (contentItem.type) {
        experienceSet.add(contentItem.type.charAt(0).toUpperCase() + contentItem.type.slice(1)); // Capitalize the first letter
      }
    });

    const languageOptions = Array.from(languageSet).map((language, index) => ({
      category: "Language",
      name: language,
      id: index + 1
    }));

    const experienceOptions = Array.from(experienceSet).map((experience, index) => ({
      category: "Experience",
      name: experience,
      id: index + 1 + languageSet.size
    }));

    return [...languageOptions, ...experienceOptions];
  };


  const setFilteredList = (selectedList) => {
    let langs = selectedList
      .filter((option) => option.category === "Language")
      .map((option) => option.name.toLowerCase()); // Convert to lowercase for case-insensitive comparison

    let exps = selectedList
      .filter((option) => option.category === "Experience")
      .map((option) => option.name.toLowerCase()); // Convert to lowercase for case-insensitive comparison

    if (exps.length === 0) {
      exps = options // Use dynamic options
        .filter((value) => value.category === "Experience")
        .map((value) => value.name.toLowerCase());
    }

    if (langs.length === 0) {
      langs = options // Use dynamic options
        .filter((value) => value.category === "Language")
        .map((value) => value.name.toLowerCase());
    }

    const filteredList = allContent.filter(
      (content) =>
        langs.includes(content.language.toLowerCase()) && // Convert to lowercase for case-insensitive comparison
        exps.includes(content.type.toLowerCase()) // Convert to lowercase for case-insensitive comparison
    );
    setContent(sortContentByCreationTime(filteredList));
  };

  useEffect(() => {
    const getContent = async () => {
      const contentFromServer = await getAllContent();
      // filter by isDeleted is False
      const contentFromServerNotDeleted = contentFromServer.filter((content) => !content.isDeleted);
      setAllContent(sortContentByCreationTime(contentFromServerNotDeleted));
      setContent(sortContentByCreationTime(contentFromServerNotDeleted));
      setOptions(generateOptions(sortContentByCreationTime(contentFromServerNotDeleted)));
    };
    getContent();
  }, []);

  const getAllContent = async () => {

    const seedsRes = await fetch(
      `${SEEDS_URL}/content`,
      {
        method: "GET",
        headers: {
          authToken: "postman",
        },
      }
    );
    const seedsData = await seedsRes.json();

    // const res = await fetch(
    //   "https://place-seeds.azurewebsites.net/getAllQuizzes"
    // );
    // const data = await res.json();
    // let quizData = data["quizzes"];
    // quizData = quizData.map((quiz) => ({ ...quiz, type: "quiz" }));
    // seedsData.push(...quizData);
    return seedsData;
  };

  const onDelete = async (type, id) => {
    console.log(id);
    if (window.confirm("Are you sure?")) {
      if (type == "quiz") {
        await fetch(
          "https://place-seeds.azurewebsites.net/byId?" +
          new URLSearchParams({
            id: id,
            type: "quiz",
          }),
          {
            method: "DELETE",
          }
        );
      } else {
        await fetch(
          `${SEEDS_URL}/content/${id}`,
          {
            method: "DELETE",
            headers: {
              authToken: "postman",
            },
          }
        );
      }
      setContent(sortContentByCreationTime(content.filter((content) => content.id != id)));
    }
  };

  const onView = (type, id) => {
    navigate(`/content/detail/${type}/${id}`);
  };
  const onEdit = (type, id) => {
    navigate(`/content/edit/${type}/${id}`);
  };

  return (
    <div style={{ margin: "30px" }}>
      <h2 style={{ color: "#28574F" }}>Hi {currentUser}!</h2>
      <h4 style={{ color: "#28574F" }}>Here is the SEEDS content dashboard</h4>
      <br />
      <LogoutButton />
      <div>
        <Link to="/ivr">
          <button
            className="btn"
            style={{ backgroundColor: "#28574F", color: "white" }}
          >
            {" "}
            IVR Usage
          </button>
        </Link>
      </div>

      <br />
      <br>
      </br>
      <div className="align-items-end">
        <Link to="/viewivr">
          <button
            className="btn"
            style={{ backgroundColor: "#28574F", color: "white" }}
          >
            {" "}
            Visualise IVR
          </button>
        </Link>
      </div>
      
      <br>
      </br>
      <div className="align-items-end">
        <Link to="/bulkcall">
          <button
            className="btn"
            style={{ backgroundColor: "#28574F", color: "white" }}
          >
            {" "}
            Mass call
          </button>
        </Link>
      </div>
      


      <Multiselect
        options={options} // Use dynamic options
        onSelect={(selectedList) => setFilteredList(selectedList)}
        onRemove={(selectedList) => setFilteredList(selectedList)}
        displayValue="name"
        groupBy="category"
        style={{
          chips: {
            background: "#28574f",
          },
          multiselectContainer: {
            color: "#28574f",
          },
        }}
      />

      <br />

      <div className="align-items-end">
        <Link to="/content/create">
          <button
            className="btn"
            style={{ backgroundColor: "#28574F", color: "white" }}
          >
            {" "}
            + Add Content
          </button>
        </Link>
      </div>
      <br></br>

      <button
        className="btn"
        style={{ backgroundColor: "#28574F", color: "white" }}
        onClick={onUpdateIVR}
      >
        Update IVR
      </button>
      <span>{updateIVRStatus}</span>

      <br>
      </br>

      {content.length == 0 && <h3>No content found :( </h3>}
      <div className="row">
        {content.length > 0 && <table className="table table-striped table-bordered">
          <thead>
            <tr className="tableHeading">
              <th style={{ color: "white", backgroundColor:"#28574f" }}> TITLE </th>
              <th style={{ color: "white", backgroundColor:"#28574f" }}> THEME </th>
              <th style={{ color: "white", backgroundColor:"#28574f" }}> UPLOADED </th>
              <th style={{ color: "white", backgroundColor:"#28574f" }}> LANGUAGE </th>
              <th style={{ color: "white", backgroundColor:"#28574f" }}> TYPE </th>
              <th style={{ color: "white", backgroundColor:"#28574f" }}> ACTIONS </th>
            </tr>
          </thead>
          <tbody>
            {content.map((content) => (
              <tr key={content.id}>
                <td> {content.title} <br /> {content.localTitle} </td>
                <td> {content.theme} <br /> {content.localTheme} </td>
                <td>{content.isTeacherApp && 'TA'}{content.isPullModel && ', IVR'} {content.type == 'quiz' && 'IVR'}</td>
                <td> {content.language}</td>
                <td> {content.type}</td>
                <td>
                  <button
                    onClick={() => onEdit(content.type, content.id)}
                    className="btn rounded"
                    style={{ backgroundColor: "#E5A83B", color: "white" }}
                  >
                    Edit{" "}
                  </button>
                  <button
                    style={{
                      marginLeft: "10px",
                      backgroundColor: "#039DCE",
                      color: "white",
                    }}
                    onClick={() => onView(content.type, content.id)}
                    className="btn"
                  >
                    View{" "}
                  </button>
                  <button
                    style={{ marginLeft: "10px" }}
                    onClick={() => onDelete(content.type, content.id)}
                    className="btn btn-danger"
                  >
                    Delete{" "}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>}
      </div>
    </div>
  );
};

export default AllContent;
