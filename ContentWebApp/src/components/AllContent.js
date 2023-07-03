import React from "react";
import Content from "./Content";
import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import Multiselect from "multiselect-react-dropdown";
import { SEEDS_URL } from "../Constants";
import {useLocation} from 'react-router-dom';
import LogoutButton from "./LogoutButton";
import { getAuth, onAuthStateChanged } from 'firebase/auth';


const AllContent = () => {
  const [content, setContent] = useState([]);
  const [allContent, setAllContent] = useState([]);
  const navigate = useNavigate();

  const [currentUser, setCurrentUser] = useState("");

  useEffect(() => {
    const auth = getAuth();
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setCurrentUser(user.displayName);
    });

    return () => unsubscribe(); // Clean up the listener when the component unmounts
  }, []);

  const st = {
    options: [
      { category: "Language", name: "Kannada", id: 1 },
      { category: "Language", name: "Hindi", id: 2 },
      { category: "Language", name: "English", id: 3 },
      { category: "Language", name: "Marathi", id: 4 },
      { category: "Language", name: "Tamil", id: 5 },
      { category: "Experience", name: "Story", id: 6 },
      { category: "Experience", name: "Poem", id: 7 },
      { category: "Experience", name: "Song", id: 8 },
      { category: "Experience", name: "Quiz", id: 9 },
      {category: "Language", name: "Bengali", id: 10},
    ],
  };

  const setFilteredList = (selectedList) => {
    var langs = selectedList
      .filter((option) => option.category == "Language")
      .map((option) => option.name.toLowerCase());
    var exps = selectedList
      .filter((option) => option.category == "Experience")
      .map((option) => option.name.toLowerCase());

    console.log("SELECTED CONTENT", langs, exps);

    if (exps.length == 0) {
      exps = st.options
        .filter((value) => value.category == "Experience")
        .map((value) => value.name.toLowerCase());
    }

    if (langs.length == 0) {
      langs = st.options
        .filter((value) => value.category == "Language")
        .map((value) => value.name.toLowerCase());
    }

    const filteredList = allContent.filter(
      (content) =>
        langs.includes(content.language) &&
        exps.includes(content.type.toLowerCase())
    );
    setContent(filteredList);
  };

  const onSelect = (selectedList, selectedItem) => {
    console.log("ON SELECT SELECTED LIST", selectedList);
    setFilteredList(selectedList);
  };

  const onRemove = (selectedList, removedItem) => {
    setFilteredList(selectedList);
    console.log("ON REMOVE SELECTED LIST", selectedList);
  };

  useEffect(() => {
    const getContent = async () => {
      const contentFromServer = await getAllContent();
      setAllContent(contentFromServer);
      setContent(contentFromServer);
    };
    getContent();
  }, []);

  const getAllContent = async () => {
    // const seedsRes = await fetch(
    //   "https://seeds-teacherapp.azurewebsites.net/content",
    //   {
    //     method: "GET",
    //     headers: {
    //       authToken: "postman",
    //     },
    //   }
    // );

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

    const res = await fetch(
      "https://place-seeds.azurewebsites.net/getAllQuizzes"
    );
    const data = await res.json();
    let quizData = data["quizzes"];
    quizData = quizData.map((quiz) => ({ ...quiz, type: "quiz" }));
    seedsData.push(...quizData);
    return seedsData;
  };

  const onDelete = async (type, id) => {
    console.log(id);
    if (window.confirm("Do you want to remove?")) {
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
      setContent(content.filter((content) => content.id != id));
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
      <br/>
      <LogoutButton/>
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
      
      <br/>

      <Multiselect
        options={st.options} // Options to display in the dropdown
        selectedValues={st.selectedValue} // Preselected value to persist in dropdown
        onSelect={onSelect} // Function will trigger on select event
        onRemove={onRemove} // Function will trigger on remove event
        displayValue="name" // Property name to display in the dropdown options
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

      <br/>

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
      {content.length == 0 && <h3>No content found :( </h3>}
      <div className="row">
        {content.length > 0 && <table className="table table-striped table-bordered">
          <thead>
            <tr className="tableHeading">
              <th style={{ color: "white" }}> TITLE </th>
              <th style={{ color: "white" }}> THEME </th>
              <th style={{ color: "white" }}> UPLOADED </th>
              <th style={{ color: "white" }}> LANGUAGE </th>
              <th style={{ color: "white" }}> TYPE </th>
              <th style={{ color: "white" }}> ACTIONS </th>
            </tr>
          </thead>
          <tbody>
            {content.map((content) => (
              <tr key={content.id}>
                <td> {content.title} <br/> {content.localTitle} </td>
                <td> {content.theme} <br/> {content.localTheme} </td>
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
