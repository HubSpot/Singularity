import React from 'react';
import { connect } from 'react-redux';

import Breadcrumbs from '../common/Breadcrumbs';
import JSONButton from '../common/JSONButton';
import Section from '../common/Section';
import CollapsableSection from '../common/CollapsableSection';

class TaskDetail extends React.Component {

  renderHeader(t) {
    return (
      <header className='detail-header'>
        <div className="row">
          <div className="col-md-12">
            <Breadcrumbs
              items={[
                {
                  label: "Request",
                  text: t.task.taskId.requestId,
                  link: `${config.appRoot}/request/${t.task.taskId.requestId}`
                },
                {
                  label: "Deploy",
                  text: t.task.taskId.deployId,
                  link: `${config.appRoot}/request/${t.task.taskId.requestId}/deploy/${t.task.taskId.deployId}`
                },
                {
                  label: "Task",
                  text: t.task.taskId.id,
                }
              ]}
            />
          </div>
        </div>
      </header>
    );
  }

  render() {
    let task = this.props.task[this.props.taskId].data;
    console.log(task);
    return (
      <div>
        {this.renderHeader(task)}
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    task: state.api.task
  };
}

export default connect(mapStateToProps)(TaskDetail);
