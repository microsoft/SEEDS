import React, { useState, useEffect } from 'react';
import * as XLSX from 'xlsx';
import { SEEDS_URL } from "../Constants";

// const SEEDS_URL = 'your_seeds_url'; // replace with your SEEDS_URL

const BulkCallInitiator = () => {
    const [phoneNumbers, setPhoneNumbers] = useState([]);
    const [contentList, setContentList] = useState([]);
    const [selectedContents, setSelectedContents] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');

    // Fetch content IDs
    useEffect(() => {
        const fetchContents = async () => {
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
            setContentList(seedsData); // Assuming seedsData is an array of contents
        };

        fetchContents();
    }, []);

    const handleFileUpload = (e) => {
        const file = e.target.files[0];
        console.log('File:', file);
        const reader = new FileReader();
        reader.onload = (evt) => {
            const data = new Uint8Array(evt.target.result);
            const workbook = XLSX.read(data, { type: 'array' });
            const sheetName = workbook.SheetNames[0];
            const sheet = workbook.Sheets[sheetName];
            const parsedData = XLSX.utils.sheet_to_json(sheet, { header: 1 });
            console.log('Parsed Data:', parsedData);
            const phoneNumbers = parsedData.map(row => row[0]);
            
            console.log('Extracted Phone Numbers:', phoneNumbers);
            // const phoneNumbers = parsedData.flat().filter(item => typeof item === 'string');
            // console.log('Extracted Phone Numbers:', phoneNumbers);
            setPhoneNumbers(phoneNumbers);
        };
        reader.readAsArrayBuffer(file);
    };

    const handleContentSelect = (id) => {
        setSelectedContents((prev) => {
            if (prev.includes(id)) {
                return prev.filter((contentId) => contentId !== id);
            } else {
                return [...prev, id];
            }
        });
    };

    const handleStartCalls = async () => {
        const response = await fetch('/start_bulk_calls', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                phone_numbers: phoneNumbers,
                content_ids: selectedContents,
            }),
        });

        if (response.ok) {
            alert('Calls initiated successfully.');
        } else {
            const errorData = await response.json();
            console.error(errorData);
            alert('Failed to initiate calls.');
        }
    };

    return (
        <div style={{padding: "20px"}}>
            <h3>Bulk Call Initiator</h3>

            <h4>Upload Phone Numbers</h4>
            <input type="file" accept=".xlsx, .xls" onChange={handleFileUpload} />
            <ul>
                {phoneNumbers.map((phone, index) => (
                    <li key={index}>{phone}</li>
                ))}
            </ul>

            <h4>Select Contents</h4>
            <input
                type="text"
                placeholder="Search contents..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
            />
            <ul>
                {contentList
                    .filter((content) => content.title.toLowerCase().includes(searchTerm.toLowerCase()))
                    .map((content) => (
                        <li key={content.id}>
                            <input
                                type="checkbox"
                                checked={selectedContents.includes(content.id)}
                                onChange={() => handleContentSelect(content.id)}
                            />
                            {content.title}
                        </li>
                    ))}
            </ul>

            <h3>Final Confirmation</h3>
            <h4>Phone Numbers</h4>
            <ul>
                {phoneNumbers.map((phone, index) => (
                    <li key={index}>{phone}</li>
                ))}
            </ul>
            <h4>Selected Contents</h4>
            <ul>
                {selectedContents.map((id) => {
                    const content = contentList.find((content) => content.id === id);
                    return <li key={id}>{content.title}</li>;
                })}
            </ul>

            <button onClick={handleStartCalls}>Start Calls</button>
        </div>
    );
};

export default BulkCallInitiator;
