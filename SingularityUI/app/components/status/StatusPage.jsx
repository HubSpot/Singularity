import React from 'react';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { Link } from 'react-router';
import rootComponent from '../../rootComponent';
import { refresh } from '../../actions/ui/status';

import HostStates from './HostStates';
import StatusList from './StatusList';
import Breakdown from '../common/Breakdown';
import Utils from '../../utils';

const StatusPage = (props) => {
  const renderPercentage = (number, total) => number > 0 && `(${Math.round(number / total * 100)}%)`;

  const renderTaskLag = (status) => status.maxTaskLag > 0 && (<h4>Max Task Lag: {Utils.duration(status.maxTaskLag)}</h4>);

  const requestDetail = (status) => {
    const totalRequests = status.activeRequests + status.pausedRequests + status.cooldownRequests + status.pendingRequests + status.cleaningRequests;

    const requests = [
      {
        type: 'success',
        attribute: 'activeRequests',
        label: 'active',
        count: status.activeRequests,
        percent: status.activeRequests / totalRequests * 100,
        link: '/requests/all/active'
      },
      {
        type: 'disabled',
        attribute: 'pausedRequests',
        label: 'paused',
        count: status.pausedRequests,
        percent: status.pausedRequests / totalRequests * 100,
        link: '/requests/all/paused'
      },
      {
        type: 'info',
        attribute: 'cooldownRequests',
        label: 'cooling down',
        count: status.cooldownRequests,
        percent: status.cooldownRequests / totalRequests * 100,
        link: '/requests/all/cooldown'
      },
      {
        type: 'waiting',
        attribute: 'pendingRequests',
        label: 'pending',
        count: status.pendingRequests,
        percent: status.pendingRequests / totalRequests * 100,
        link: '/requests/all/pending'
      },
      {
        type: 'warning',
        attribute: 'cleaningRequests',
        label: 'cleaning',
        count: status.cleaningRequests,
        percent: status.cleaningRequests / totalRequests * 100,
        link: '/requests/all/cleaning'
      },
    ];

    return ({
      requests,
      totalRequests
    });
  };

  const taskDetail = (status) => {
    const totalTasks = status.activeTasks + status.launchingTasks + status.lateTasks + status.scheduledTasks + status.cleaningTasks + status.lbCleanupTasks;
    const tasks = [
      {
        type: 'success',
        attribute: 'activeTasks',
        label: 'active',
        count: status.activeTasks,
        percent: status.activeTasks / totalTasks * 100,
        link: '/tasks'
      },
      {
        type: 'info',
        attribute: 'launchingTasks',
        label: 'launching',
        count: status.launchingTasks,
        percent: status.launchingTasks / totalTasks * 100,
        link: '/tasks'
      },
      {
        type: 'waiting',
        attribute: 'scheduledTasks',
        label: 'scheduled',
        count: status.scheduledTasks,
        percent: status.scheduledTasks / totalTasks * 100,
        link: '/tasks/scheduled'
      },
      {
        type: 'danger',
        attribute: 'lateTasks',
        label: 'overdue',
        count: status.lateTasks,
        percent: status.lateTasks / totalTasks * 100,
        link: '/tasks/scheduled'
      },
      {
        type: 'warning',
        attribute: 'cleaningTasks',
        label: 'cleaning',
        count: status.cleaningTasks,
        percent: status.cleaningTasks / totalTasks * 100,
        link: '/tasks/cleaning'
      },
      {
        type: 'warning-strong',
        attribute: 'lbCleanupTasks',
        label: 'load balancer cleanup',
        count: status.lbCleanupTasks,
        percent: status.lbCleanupTasks / totalTasks * 100,
        link: '/tasks/lbcleanup'
      }
    ];

    return ({
      tasks,
      totalTasks
    });
  };

  const getRequestsData = (status) => status.requests.map((request) => ({
    component: (className) => (
      <Link to={request.link} className={className}>
        {request.count} {request.label} {renderPercentage(request.count, status.totalRequests)}
      </Link>
    ),
    beforeFill: request.type,
    value: request.count,
    id: request.type
  }));

  const getTasksData = (status) => status.tasks.map((task) => ({
    component: (className) => (
      <Link to={task.link} className={className}>
        {task.count} {task.label} {renderPercentage(task.count, status.totalTasks)}
      </Link>
    ),
    beforeFill: task.type,
    value: task.count,
    id: task.type
  }));

  const status = Utils.deepClone(props.status);

  status.isLeaderConnected = false;
  status.hasLeader = false;
  for (const host in status.hostStates) {
    if (host.driverStatus === 'DRIVER_RUNNING') {
      status.hasLeader = true;
      if (host.mesosConnected) status.isLeaderConnected = true;
    }
  }
  _.extend(status, requestDetail(status));
  _.extend(status, taskDetail(status));

  return (
    <div>
      <div className="row">
        <div className="col-sm-12 col-md-6">
          <h2>Requests</h2>
          <div className="row">
            <div className="col-md-3 col-sm-3 hidden-xs chart">
              <Breakdown total={status.allRequests} data={status.requests} />
            </div>
            <div className="col-md-9 col-sm-9">
              <StatusList data={getRequestsData(status)} />
            </div>
          </div>
        </div>
        <div className="col-sm-12 col-md-6">
          <h2>Tasks</h2>
            <div className="row">
              <div className="col-md-3 col-sm-3 hidden-xs chart">
                <Breakdown total={status.totalTasks} data={status.tasks} />
              </div>
              <div className="col-md-9 col-sm-9">
                <StatusList data={getTasksData(status)} />
                {renderTaskLag(status)}
              </div>
          </div>
        </div>
      </div>
      <div className="row">
        <div className="col-md-4 col-sm-12">
          <StatusList
            header="Racks"
            data={[
              {
                component: (className) => (
                  <Link to="racks/active" className={className}>
                    {status.activeRacks} Active Racks
                  </Link>
                ),
                id: 'activeracks',
                value: status.activeRacks
              },
              {
                component: (className) => (
                  <Link to="racks/decommission" className={className}>
                    {status.decomissioningRacks} Decommissioning Racks
                  </Link>
                ),
                id: 'decomracks',
                value: status.decomissioningRacks
              },
              {
                component: (className) => (
                  <Link to="racks/inactive" className={className}>
                    {status.deadRacks} Inactive Racks
                  </Link>
                ),
                id: 'inactiveracks',
                value: status.deadRacks
              }
            ]}
          />
        </div>
        <div className="col-md-4 col-sm-12">
          <StatusList
            header="Slaves"
            data={[
              {
                component: (className) => (
                  <Link to="slaves/active" className={className}>
                    {status.activeSlaves} Active Slaves
                  </Link>
                ),
                value: status.activeSlaves,
                id: 'activeslaves'
              },
              {
                component: (className) => (
                  <Link to="slaves/decommission" className={className}>
                    {status.decomissioningSlaves} Decommissioning Slaves
                  </Link>
                ),
                value: status.decomissioningSlaves,
                id: 'decomslaves'
              },
              {
                component: (className) => (
                  <Link to="slaves/inactive" className={className}>
                    {status.deadSlaves} Inactive Slaves
                  </Link>
                ),
                className: status.deadSlaves > 0 ? 'color-warning' : '',
                value: status.deadSlaves,
                id: 'deadslaves'
              },
              status.unknownSlaves ? {
                component: (className) => (
                  <Link to="slaves/inactive" className={className}>
                    {status.unknownSlaves} Unknown Slaves
                  </Link>
                ),
                className: 'color-warning',
                value: status.unknownSlaves,
                id: 'unknownslaves'
              } : null
            ]}
          />
        </div>
        <div className="col-md-4 col-sm-12">
          <StatusList
            header="Deploys"
            data={[
              {
                component: (className) => (
                  <span className={classNames(className, status.numDeploys < 2 && 'text-muted')}>
                    <strong>{status.numDeploys}</strong> Active Deploys
                  </span>
                ),
                value: status.numDeploys,
                id: 'numdeploys'
              },
              status.oldestDeploy !== 0 ? {
                component: (className) => (
                  <span className={className}>
                    <strong>{Utils.duration(status.oldestDeploy)}</strong> since last deploy
                  </span>
                )
              } : null
            ]}
          />
        </div>
      </div>
      <div className="row">
        <div className="col-sm-12">
          <HostStates hosts={status.hostStates} />
        </div>
      </div>
    </div>
  );
};

StatusPage.propTypes = {
  status: React.PropTypes.object
};

function mapStateToProps(state) {
  return {
    status: state.api.status.data
  };
}

export default connect(mapStateToProps)(rootComponent(StatusPage, refresh));
