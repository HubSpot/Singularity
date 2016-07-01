import React from 'react';
import {reduxForm} from 'redux-form';
import classNames from 'classnames';
import { Panel, Button } from 'react-bootstrap';
import DateTimeField from 'react-bootstrap-datetimepicker';
import moment from 'moment';

import ReduxSelect from '../common/formItems/ReduxSelect';
import Utils from '../../utils';

class TaskSearchFilters extends React.Component {

  handleSubmit(e) {
    e.preventDefault();
    if (this.props.valid) {
      const result = _.mapObject(this.props.fields, (v, k) => v.value);
      this.props.onSearch(result);
    }
  }

  renderStatusOptions(opt) {
    return (
      <span style={{fontSize: '14px'}} className={`label label-${Utils.getLabelClassFromTaskState(opt.value)}`}>
        {opt.label}
      </span>
    );
  }

  render() {
    const {fields: {requestId, deployId, host, startedAfter, startedBefore, lastTaskStatus}} = this.props;
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
            <div className="form-group col-md-4">
              <label for="requestId">Request ID</label>
              <input className="form-control" disabled {...requestId} />
            </div>
            <div className="form-group col-md-4">
              <label for="deployId">Deploy ID</label>
              <input className="form-control" {...deployId} />
            </div>
            <div className="form-group col-md-4">
              <label for="host">Host</label>
              <input className="form-control" {...host} />
            </div>
          </div>
          <div className="row">
            <div className={classNames("form-group col-md-4", {"has-error": startedAfter.error || startedBefore.error})}>
              <label className="control-label">Started Between</label>
              <div className="row">
                <div className="col-md-6">
                  <DateTimeField defaultText="" maxDate={moment()} {...startedAfter} />
                </div>
                <div className="col-md-6">
                  <DateTimeField defaultText="" minDate={moment(startedAfter.value ? parseInt(startedAfter.value) : moment(0))} maxDate={moment()} {...startedBefore} />
                </div>
              </div>
              <span className="text-center help-block">{startedAfter.error || startedBefore.error}</span>
            </div><div className="form-group col-md-4">
              <label for="lastTaskStatus">Last Task Status</label>
              <ReduxSelect options={statusOptions} optionRenderer={this.renderStatusOptions} valueRenderer={this.renderStatusOptions} {...lastTaskStatus} />
            </div>
            <div className="col-md-4 text-right">

            </div>
          </div>
          <Button type="submit" bsStyle="primary" className="pull-right" disabled={!this.props.valid}>Submit</Button>
          <Button type="button" bsStyle="default" className="pull-right" onClick={() => this.props.resetForm()}>Clear</Button>
        </form>
      </Panel>
    );
  }
}

const validate = values => {
  const errors = {}
  if (values.dateStart && !moment(parseInt(values.dateStart)).isValid()) {
    errors.dateStart = "Please enter a valid date";
  }
  if (values.dateEnd && !moment(parseInt(values.dateEnd)).isValid()) {
    errors.dateEnd = "Please enter a valid date";
  }
  if (values.dateStart && values.dateEnd && parseInt(values.dateEnd) < parseInt(values.dateStart)) {
    errors.dateEnd = "End date must be after start";
  }
  return errors
}

function mapStateToProps(state, ownProps) {
  return {
    initialValues: {
      requestId: ownProps.requestId,
      dateStart: null,
      dateEnd: null
    }
  }
}

export default reduxForm({
  form: 'taskSearch',
  fields: ['requestId', 'deployId', 'host', 'startedAfter', 'startedBefore', 'lastTaskStatus'],
  validate
}, mapStateToProps)(TaskSearchFilters);
