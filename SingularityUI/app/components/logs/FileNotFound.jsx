import React from 'react';

class FileNotFound extends React.Component {
  render() {
    return <div className="lines-wrapper"><div className="empty-table-message"><p>{_.last(this.props.fileName.split('/'))}does not exist{this.props.fileName && this.props.fileName.indexOf('$TASK_ID') !== -1 ? " in this task's directory" : ' for this task'}.</p></div></div>;
  }
}

export default FileNotFound;

