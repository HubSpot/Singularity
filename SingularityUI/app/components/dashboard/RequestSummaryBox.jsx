import React, { Component, PropTypes } from 'react';
import { Glyphicon, PanelGroup, Panel, Well, Button, Col, Row } from 'react-bootstrap'
import { Link } from 'react-router';

import Utils from '../../utils';

import UITable from '../common/table/UITable';
import {
  Health,
  InstanceNumberWithLink,
  DeployId,
  StartedAt,
  UpdatedAt,
  LogLinkAndActions
} from '../tasks/Columns';

export default class RequestSummaryBox extends Component {
  static propTypes = {
    request: PropTypes.object.isRequired,
    currentEvent: PropTypes.object.isRequired
  };

  constructor() {
    super();
    this.state = {
      showTasks: false
    };
  }

  getActivityPanelStyle(request) {
    if (request.state === "ACTIVE") {
      if (request.hasActiveDeploy) {
        return "info";
      } else {
        return "default";
      }
    } else if (request.state === "PAUSED") {
      return "warning";
    } else {
      return "info";
    }
  }

  getTasksPanelStyle(request) {
    let healthyTasks = 0;
    healthyTasks += this.props.request.taskIds && this.props.request.taskIds.healthy.length || 0
    healthyTasks += this.props.request.taskIds && this.props.request.taskIds.cleaning.length || 0
    if (request.instances > healthyTasks) {
      return "warning"
    } else if (!(this.props.request.requestDeployState && this.props.request.requestDeployState.activeDeploy)) {
      return "default"
    } else {
      return "info"
    }
  }

  getTasksHeader(taskIds) {
    const cleaning = this.props.request.taskIds && this.props.request.taskIds.cleaning.length || 0
    const healthy = this.props.request.taskIds && this.props.request.taskIds.healthy.length || 0
    const notYetHealthy = this.props.request.taskIds && this.props.request.taskIds.notYetHealthy.length || 0
    const pending = this.props.request.taskIds && this.props.request.taskIds.pending.length || 0

    if (cleaning == 0 && healthy == 0 && notYetHealthy == 0 && pending == 0) {
      return "No Tasks"
    } else {
      let header = "Tasks -";
      if (healthy > 0) {
        header = header + ` ${healthy} Healthy `
      }
      if (notYetHealthy > 0) {
        header = header + ` ${notYetHealthy} Not Yet Healthy `
      }
      if (cleaning > 0) {
        header = header + ` ${cleaning} Cleaning `
      }
      if (pending > 0) {
        header = header + ` ${pending} Pending`
      }
      return header
    }
  }

  render() {
    const tasks = [];
    _.each(this.props.request.taskIds, (taskIds, status) => {
      tasks.push.apply(tasks, taskIds.map((taskId) => {
        return {
          taskId: taskId,
          instanceNo: taskId.instanceNo,
          health: status
        }
      }));
    });
    
    let activeDeploy;
    if (this.props.request.requestDeployState && this.props.request.requestDeployState.activeDeploy) {
      activeDeploy = (
        <span>Active Deploy (
          <Link to={`request/${this.props.request.id}/deploy/${this.props.request.requestDeployState.activeDeploy.deployId}`}>
            {this.props.request.requestDeployState.activeDeploy.deployId}
          </Link>
          )
        </span>
      )
    } else {
      activeDeploy = "No Active Deploy"
    }

    return (
      <div className="request-summary">
        <Row>
          <Col md={3}>
            {activeDeploy}
          </Col>
          <Col md={9}>
            {this.props.currentEvent}
          </Col>
        </Row>
        {tasks.length > 0 &&
          <Row>
            <Col md={12}>
              <Button className="tasks-button btn-block" onClick={() => this.setState({ showTasks: !this.state.showTasks })}>
                <Glyphicon glyph={this.state.showTasks ? "collapse-up" : "collapse-down"}/> {`${this.getTasksHeader(this.props.request.taskIds)}`}
              </Button>
              <Panel
                bsStyle={this.getTasksPanelStyle(this.props.request)}
                expanded={this.state.showTasks}
                collapsible
              >
                <UITable
                  data={tasks}
                  keyGetter={(task) => task.taskId.id}
                  emptyTableMessage='No tasks'
                  defaultSortBy="instanceNo"
                >
                  {Health}
                  {InstanceNumberWithLink}
                  {DeployId}
                  {StartedAt}
                  {UpdatedAt}
                  {LogLinkAndActions(config.runningTaskLogPath, this.props.request.requestType)}
                </UITable>
              </Panel>
            </Col>
          </Row>
        }
      </div>
    );
  }
}
