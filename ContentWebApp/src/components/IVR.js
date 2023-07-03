import { useState, useEffect } from "react";
import Papa from "papaparse";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";

const IVR = () => {
  const [schools, setSchools] = useState([]);
  const [selectedSchool, setSelectedSchool] = useState('');
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedEndDate, setSelectedEndDate] = useState(
    null
  );
  const [contentUsage, setContentUsage] = useState(null);
  const [studentData, setStudentData] = useState([]);

  function msToTime(s) {
    var ms = s % 1000;
    s = (s - ms) / 1000;
    var secs = s % 60;
    s = (s - secs) / 60;
    var mins = s % 60;
    var hrs = (s - mins) / 60;
    if (mins == 0) return secs + " s";
    return mins + " mins " + secs + " s";
  }

  // useEffect(() => {
  //   async function fetchData() {
  //     const response = await fetch("/StudentData.csv");
  //     const reader = response.body.getReader();
  //     const result = await reader.read();
  //     const decoder = new TextDecoder("utf-8");
  //     const csv = decoder.decode(result.value);
  //     const results = Papa.parse(csv, { header: true }).data;
  //     const uniqueSchools = [...new Set(results.map((row) => row["School"]))];
  //     console.log(uniqueSchools);
  //     setSchools(uniqueSchools);
  //     setStudentData(results);
  //   }
  //   fetchData();
  // }, []);

  const handleFileUpload = (event) => {
    const file = event.target.files[0];
    if (!file) {
      setStudentData([]);
      return;
    }
    Papa.parse(file, {
      header: true,
      complete: (results) => {
        const uniqueSchools = [
          ...new Set(results.data.map((row) => row["School"])),
        ];
        const uniqueSchoolsNonEmpty = uniqueSchools.filter(
          (school) => school !== undefined
        );
        setSchools(uniqueSchoolsNonEmpty);
        setStudentData(results.data);
        console.log("UNIQUE SCHOOLS", uniqueSchoolsNonEmpty);
        console.log("CSV DATA", results.data);
      },
    });
  };

  useEffect(() => {
    if (!studentData) {
      return;
    }

    // Make API call to fetch content usage data
    const fetchData = async () => {
      if (selectedDate && selectedEndDate) {
        const startDate = new Date(selectedDate);
        const endDate = new Date(selectedEndDate);
        if (endDate <= startDate) {
          console.log("END DATE SHOULD BE GREATER THAN START DATE");
          return;
        }
        const dates = [];

        // Loop through the dates
        const currentDate = new Date(startDate);
        while (currentDate <= endDate) {
          dates.push(new Date(currentDate).toLocaleDateString());
          currentDate.setDate(currentDate.getDate() + 1);
        }
        console.log("DATES", dates);
      
      const seedsRes = await fetch(
        `https://seeds-ivr.centralindia.azurecontainer.io/mono_call/insights`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ dates: dates }),
        }
      );
      const seedsData = await seedsRes.json();
      console.log("CREATED", seedsData);
      // const userInsights =
        // seedsData[selectedDate.toLocaleDateString()].userInsights;
      // console.log("USER INSIGHTS", userInsights);
      var dp = [];
      Object.values(seedsData).map((dateData) => {
          const userInsights = dateData.userInsights;
          const dateProcessed = userInsights.map((userInsight) => {
            const phoneNumber = userInsight.phoneNumber.substring(2);
            const totalDuration = msToTime(userInsight.callDuration);
            const content = userInsight.audioContentDurations.map(
              (content) => {
                return {
                  title: content.contentName,
                  duration: msToTime(content.duration),
                };
              }
            );
            const name = studentData.find(
              (student) => student["Student Phone Number"] === phoneNumber
            )
              ? studentData.find(
                  (student) => student["Student Phone Number"] === phoneNumber
                )["Student name"]
              : null;

            const school = studentData.find(
              (student) => student["Student Phone Number"] === phoneNumber
            )
              ? studentData.find(
                  (student) => student["Student Phone Number"] === phoneNumber
                )["School"]
              : null;
            // console.log("NAME", name, "PHONE NUMBER", phoneNumber);
            return {
              date: dateData.date,
              name: name,
              school,
              phoneNumber,
              totalDuration,
              content,
            };
          });
          dp.push(...dateProcessed);
      });
      if (!selectedSchool) {
        setContentUsage(dp);
        return;
      }

      const dataProcessedCorrectSchool = dp.filter((userInsight) => {
        const phoneNumber = userInsight.phoneNumber;
        console.log("PHONE NUMBER", phoneNumber);
        const school = studentData.find(
          (student) => student["Student Phone Number"] === phoneNumber
        )
          ? studentData.find(
              (student) => student["Student Phone Number"] === phoneNumber
            )["School"]
          : null;
        return school === selectedSchool;
      });
      // console.log("DATA PROCESSED", dataProcessed);
      console.log("DATA PROCESSED CORRECT SCHOOL", dataProcessedCorrectSchool);
      setContentUsage(dataProcessedCorrectSchool);
      }
    }

  //     const dataProcessed = userInsights.map(([key, userInsight]) => {
  //       console.log("DATE", key)
  //       console.log("USER INSIGHT", userInsight);
  //       userInsight = userInsight.userInsights;
        
  //       const date = key
  //       const phoneNumber = userInsight.phoneNumber.substring(2);
  //       const totalDuration = msToTime(userInsight.callDuration);
  //       const content = userInsight.audioContentDurations.map((content) => {
  //         return {
  //           title: content.contentName,
  //           duration: msToTime(content.duration),
  //         };
  //       });
  //       const name = studentData.find(
  //         (student) => student["Student Phone Number"] === phoneNumber
  //       )
  //         ? studentData.find(
  //             (student) => student["Student Phone Number"] === phoneNumber
  //           )["Student name"]
  //         : null;

  //       const school = studentData.find(
  //         (student) => student["Student Phone Number"] === phoneNumber
  //       )
  //         ? studentData.find(
  //             (student) => student["Student Phone Number"] === phoneNumber
  //           )["School"]
  //         : null;
  //       // console.log("NAME", name, "PHONE NUMBER", phoneNumber);
  //       return {
  //         date,
  //         name: name,
  //         school,
  //         phoneNumber,
  //         totalDuration,
  //         content,
  //       };
  //     });

  //     //keep only those data whose phone number belongs to selected school
  //     if (!selectedSchool) {
  //       setContentUsage(dataProcessed);
  //       return;
  //     }

  //     const dataProcessedCorrectSchool = dataProcessed.filter((userInsight) => {
  //       const phoneNumber = userInsight.phoneNumber;
  //       console.log("PHONE NUMBER", phoneNumber);
  //       const school = studentData.find(
  //         (student) => student["Student Phone Number"] === phoneNumber
  //       )
  //         ? studentData.find(
  //             (student) => student["Student Phone Number"] === phoneNumber
  //           )["School"]
  //         : null;
  //       return school === selectedSchool;
  //     });
  //     console.log("DATA PROCESSED", dataProcessed);
  //     setContentUsage(dataProcessedCorrectSchool);
  //   };
  // }

    fetchData();
  }, [selectedSchool, selectedDate, selectedEndDate, studentData]);

  //   useEffect(() => {
  //     async function fetchSchools() {

  //         //path to StudentData.csv from
  //       const response = await fetch('../assets/StudentData.csv');
  //       const csvData = await response.text();
  //       const jsonData = Papa.parse(csvData, { header: true }).data;
  //         console.log("hello", jsonData);
  //       const uniqueSchools = [...new Set(jsonData.map((row) => row['School']))];
  //       console.log(uniqueSchools);
  //
  //     }

  //     fetchSchools();
  //   }, []);

  function SchoolDropdown({ schools, selectedSchool, onChange }) {
    return (
      <select className="mintgreen" value={selectedSchool} onChange={onChange}>
        <option value="">Select a School</option>
        {schools.map((school, index) => (
          <option key={index} value={school}>
            {school}
          </option>
        ))}
      </select>
    );
  }

  function handleSchoolChange(event) {
    setSelectedSchool(event.target.value);
  }

  // const data = [
  //   {
  //     name: "John Doe",
  //     phone: "123-456-7890",
  //     totalDuration: "7 hours",
  //     content: [
  //       { title: "Introduction to React", duration: "2 hours" },
  //       { title: "Advanced React Techniques", duration: "3 hours" },
  //       { title: "Advanced React Techniques 2", duration: "3 hours" },
  //     ],
  //   },
  //   {
  //     name: "Jane Smith",
  //     phone: "555-123-4567",
  //     totalDuration: "3 hours",
  //     content: [
  //       { title: "React Hooks", duration: "1.5 hours" },
  //       { title: "React Router", duration: "1.5 hours" },
  //     ],
  //   },
  // ];

  return (
    <div style={{ margin: "20px" }}>
      <h5>Add Student Data</h5>
      <input type="file" accept=".csv" onChange={handleFileUpload} />
      <br />
      <h5>Select School</h5>
      <SchoolDropdown
        schools={schools}
        selectedSchool={selectedSchool}
        onChange={handleSchoolChange}
      />
      <br />
      <h6>Select start date</h6>
      <DatePicker
        maxDate={new Date()}
        className="mintgreen"
        selected={selectedDate}
        onChange={(date) => setSelectedDate(date)}
      />
      <h6>Select end date</h6>
      <DatePicker
        maxDate={new Date()}
        className="mintgreen"
        selected={selectedEndDate}
        onChange={(date) => setSelectedEndDate(date)}
      />
      <br />
      <h5>IVR Data</h5>
      <table className="table table-bordered">
        <thead>
          <tr className="tableHeading">
            <th style={{ color: "white" }}>Date</th>
            <th style={{ color: "white" }}>Student</th>
            <th style={{ color: "white" }}>Content Titles</th>
            <th style={{ color: "white" }}>Content Durations</th>
          </tr>
        </thead>
        <tbody>
          {contentUsage &&
            contentUsage.map((person, index) => (
              <tr key={index}>
                <td>{person.date}</td>
                <td>
                  <table>
                    <tbody>
                      <tr>
                        <td>
                          <b>{person.name}</b>
                        </td>
                      </tr>
                      <tr>
                        <td>
                          <b>
                            {!selectedSchool &&
                              selectedSchool.length > 0 &&
                              "School: "}
                          </b>
                          {!selectedSchool && person.school}
                        </td>
                      </tr>
                      <tr>
                        <td>
                          <b>Call duration: </b>
                          {person.totalDuration}
                        </td>
                      </tr>
                      <tr>
                        <td>{person.phoneNumber}</td>
                      </tr>
                    </tbody>
                  </table>
                </td>
                <td>
                  <table className="table-striped">
                    <tbody>
                      {person.content.map((content, index) => (
                        <tr key={index}>
                          <td>{content.title}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </td>
                <td>
                  <table className="table-striped">
                    <tbody>
                      {person.content.map((content, index) => (
                        <tr key={index}>
                          <td>{content.duration}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </td>
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
};

export default IVR;
