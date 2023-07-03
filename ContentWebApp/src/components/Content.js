import { FaTimes } from 'react-icons/fa'

const Content = ({content, onDelete, onView, onEdit}) => {
  return (
    <div>
        <h3>{content.title}</h3>
        <p>{content.language}</p>
        <button onClick={() => onView(content.type, content.id)}>View</button>
        <button onClick={() => onEdit(content.type, content.id)}>Edit</button>
        <button onClick={() => onDelete(content.type, content.id)}>Delete</button>
    </div>
  )
}

export default Content
       