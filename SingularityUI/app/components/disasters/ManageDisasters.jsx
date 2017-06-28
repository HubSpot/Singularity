import React, { PropTypes } from 'react';
import { Panel, ListGroup, ListGroupItem } from 'react-bootstrap';
import Section from '../common/Section';
import Column from '../common/table/Column';
import UITable from '../common/table/UITable';
import DisasterButton from './DisasterButton';
import AutomatedActionsButton from './AutomatedActionsButton';
import DeletePriorityFreezeButton from './DeletePriorityFreezeButton';
import DisableTaskCreditsButton from './DisableTaskCreditsButton';
import AddTaskCreditsButton from './AddTaskCreditsButton';
import NewPriorityFreezeButton from './NewPriorityFreezeButton';
import EditPriorityFreezeButton from './EditPriorityFreezeButton';
import Utils from '../../utils';

const DISASTER_TYPES = ['EXCESSIVE_TASK_LAG', 'LOST_SLAVES', 'LOST_TASKS', 'USER_INITIATED']

function ManageDisasters (props) {
  var actionButtonClass;
  var automatedActionButtonAction;
  if (props.automatedActionsDisabled) {
    automatedActionButtonAction = "Enable"
    actionButtonClass = "btn btn-primary"
  } else {
    automatedActionButtonAction = "Disable"
    actionButtonClass = "btn btn-warning"
  }

  var priority;
  if (_.isEmpty(props.priorityFreeze)) {
    priority = (
      <div>
        <div className="row">
          <NewPriorityFreezeButton user={props.user}>
            <button
              className="btn btn-warning"
              alt="New Priority Freeze"
              title="New Priority Freeze">
              New Priority Freeze
            </button>
          </NewPriorityFreezeButton>
        </div>
        <div className="row">
          <div className="empty-table-message">No Active Priority Freeze</div>
        </div>
      </div>
    );
  } else {
    var kill = props.priorityFreeze.priorityFreeze.killTasks ? "True" : "False"
    priority = (
      <div>
        <div className="row">
          <DeletePriorityFreezeButton user={props.user} >
            <button
              className="btn btn-primary"
              alt="Remove Priority Freeze"
              title="Remove Priority Freeze"
            >
              Remove Priority Freeze
            </button>
          </DeletePriorityFreezeButton>
          <EditPriorityFreezeButton user={props.user} freeze={props.priorityFreeze.priorityFreeze}>
            <button
              className="btn btn-default"
              alt="Edit Priority Freeze"
              title="Edit Priority Freeze"
            >
            Edit Priority Freeze
            </button>
          </EditPriorityFreezeButton>
        </div>
        <div className="row">
          <Panel header="Active Priority Freeze">
            <ListGroup fill>
              <ListGroupItem>Level: {props.priorityFreeze.priorityFreeze.minimumPriorityLevel}</ListGroupItem>
              <ListGroupItem>Started At: {Utils.timestampFromNow(props.priorityFreeze.timestamp)}</ListGroupItem>
              <ListGroupItem>Kill Tasks: {kill}</ListGroupItem>
              <ListGroupItem>Action Id: {props.priorityFreeze.priorityFreeze.actionId}</ListGroupItem>
              <ListGroupItem>User: {props.priorityFreeze.user}</ListGroupItem>
              <ListGroupItem>Message: {props.priorityFreeze.priorityFreeze.message}</ListGroupItem>
            </ListGroup>
          </Panel>
        </div>
      </div>
    );
  }
  var taskCredits;
  if (props.taskCredits.enabled) {
    taskCredits = (
      <div>
        <div className="row">
          <Panel header="Task Credits">
            <p>Remaining Credits: {props.taskCredits.remaining}</p>
            <AddTaskCreditsButton user={props.user} >
              <button
                className="btn btn-primary"
                alt="Add/Enable Task Credits"
                title="Add/Enable Task Credits">
                Add Task Credits
              </button>
            </AddTaskCreditsButton>
          </Panel>
        </div>
        <div className="row">
          <DisableTaskCreditsButton user={props.user} >
            <button
              className="btn btn-primary"
              alt="Disable Task Credits"
              title="Disable Task Credits">
              Disable Task Credits
            </button>
          </DisableTaskCreditsButton>
        </div>
      </div>
    );
  } else {
    taskCredits = (
      <div>
        <div className="row">
          <AddTaskCreditsButton user={props.user} >
            <button
              className="btn btn-primary"
              alt="Add/Enable Task Credits"
              title="Add/Enable Task Credits">
              Add + Enable Task Credits
            </button>
          </AddTaskCreditsButton>
        </div>
      </div>
    );
  }

  return (
    <Section title="Manage">
      <div className="row">
        <div className="col-md-3">
          <h3>Priority Freeze</h3>
          {priority}
        </div>
        <div className="col-md-3">
          <h3>Task Credits</h3>
          {taskCredits}
        </div>
        <div className="col-md-6">
          <h3>Disasters</h3>
          <div className="row">
            <AutomatedActionsButton
              user={props.user}
              action={automatedActionButtonAction}
            >
              <button
                className={actionButtonClass}
                alt={automatedActionButtonAction}
                title={automatedActionButtonAction}>
                {automatedActionButtonAction} Automated Actions
              </button>
            </AutomatedActionsButton>
          </div>
          <UITable
            emptyTableMessage="No Disaster Data Found"
            data={props.disasters}
            keyGetter={(disaster) => disaster.type}
            defaultSortBy="type"
            defaultSortDirection={UITable.SortDirection.ASC}
          >
            <Column
              label="Type"
              id="type"
              key="type"
              sortable={true}
              sortData={(cellData, disaster) => disaster.type}
              cellData={(disaster) => Utils.humanizeText(disaster.type)}
            />
            <Column
              label="State"
              id="state"
              key="state"
              cellData={(disaster) =>
                <span className={disaster.active ? 'label label-danger' : 'label label-primary'}>
                  {disaster.active ? "Active" : "Inactive"}
                </span>
              }
            />
            <Column
              id="actions-column"
              key="actions-column"
              className="actions-column"
              cellData={(disaster) =>
                <DisasterButton
                  user={props.user}
                  action={disaster.active ? "Deactivate" : "Activate"}
                  type={disaster.type}
                >
                  <button
                    className={disaster.active ? "btn btn-primary" : "btn btn-warning"}
                    alt={disaster.active ? "Deactivate" : "Activate"}
                    title={disaster.active ? "Deactivate" : "Activate"}>
                    {disaster.active ? "Deactivate" : "Activate"}
                  </button>
                </DisasterButton>
              }
            />
          </UITable>
        </div>
      </div>
    </Section>
  );
}

ManageDisasters.propTypes = {
  disasters: PropTypes.arrayOf(PropTypes.shape({
    type: PropTypes.string.isRequired,
    active: PropTypes.bool
  })).isRequired,
  priorityFreeze: PropTypes.shape({
    priorityFreeze: PropTypes.shape({
      minimumPriorityLevel: PropTypes.number,
      killTasks: PropTypes.bool,
      message: PropTypes.string,
      actionId: PropTypes.string
    }),
    timestamp: PropTypes.number,
    user: PropTypes.string
  }),
  taskCredits: PropTypes.shape({
    enabled: PropTypes.bool,
    remaining: PropTypes.number
  }),
  automatedActionsDisabled: PropTypes.bool
};

export default ManageDisasters;
