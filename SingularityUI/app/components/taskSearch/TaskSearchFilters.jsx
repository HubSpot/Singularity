import React from 'react';
import {reduxForm} from 'redux-form';
import classNames from 'classnames';
import { Panel, Button } from 'react-bootstrap';

import ReduxSelect from '../common/formItems/ReduxSelect';
import Utils from '../../utils';

class TaskSearchFilters extends React.Component {

  handleSubmit(e) {
    e.preventDefault();
    console.log(this.props.fields);
  }

  renderStatusOptions(opt) {
    return (
      <span style={{fontSize: '14px'}} className={`label label-${Utils.getLabelClassFromTaskState(opt.value)}`}>
        {opt.label}
      </span>
    );
  }

  render() {
    const {fields: {requestId, deployId, host, lastTaskStatus}} = this.props;
    const statusOptions = [
      { value: 'TASK_ERROR', label: 'Error' },
      { value: 'TASK_FAILED', label: 'Failed' },
      { value: 'TASK_FINISHED', label: 'Finished' },
      { value: 'TASK_KILLED', label: 'Killed' },
      { value: 'TASK_LOST', label: 'Lost' },
      { value: 'TASK_LOST_WHILE_DOWN', label: 'Lost while down' },
    ];

    return (
      <Panel>
        <form onSubmit={(...args) => this.handleSubmit(...args)}>
          <div className="row">
            <div className="col-md-4">
              <label for="requestId">Request ID</label>
              <input className="form-control" disabled {...requestId} />
            </div>
            <div className="col-md-4">
              <label for="deployId">Deploy ID</label>
              <input className="form-control" {...deployId} />
            </div>
            <div className="col-md-4">
              <label for="host">Host</label>
              <input className="form-control" {...host} />
            </div>
          </div>
          <div className="row">
            <div className="col-md-4">
              <label for="startedBetween">Started Between</label>
            </div><div className="col-md-4">
              <label for="lastTaskStatus">Last Task Status</label>
              <ReduxSelect options={statusOptions} optionRenderer={this.renderStatusOptions} valueRenderer={this.renderStatusOptions} {...lastTaskStatus} />
            </div>
            <div className="col-md-4 text-right">
              <Button type="submit" bsStyle="primary" className="pull-right">Submit</Button>
              <Button type="button" bsStyle="default" className="pull-right">Clear</Button>
            </div>
          </div>
        </form>
      </Panel>
    );
  }
}

function mapStateToProps(state, ownProps) {
  return {
    initialValues: {
      requestId: ownProps.requestId
    }
  }
}

export default reduxForm({
  form: 'taskSearch',
  fields: ['requestId', 'deployId', 'host', 'lastTaskStatus']
}, mapStateToProps)(TaskSearchFilters);
