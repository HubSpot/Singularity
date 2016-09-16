import React from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import LogContainer from './LogContainer';

import LogActions from '../../actions/log';
import { updateActiveTasks } from '../../actions/activeTasks';

const TailPage = () => {
  return <LogContainer />;
};

const mapDispatchToProps = {
  updateActiveTasks,
  initialize: LogActions.initialize,
  initializeUsingActiveTasks: LogActions.initializeUsingActiveTasks
};

function getTitle(props) {
  const file = _.last(props.params.splat.split('/'));
  return `Tail of ${file}`;
}

function refreshTail(props) {
  const splits = props.params.taskId.split('-');
  const requestId = splits.slice(0, splits.length - 5).join('-');
  const search = props.location.query.search || '';
  const path = props.params.splat.replace(props.params.taskId, '$TASK_ID');
  const initPromise = props.initialize(requestId, path, search, [props.params.taskId], props.location.query.viewMode || 'split', 'SANDBOX');
  initPromise.then(() => {
    props.updateActiveTasks(requestId);
  });
}
export const Tail = connect(null, mapDispatchToProps)(rootComponent(TailPage, getTitle, refreshTail, false, false));

function refreshCompressedLog(props) {
  const splits = props.params.taskId.split('-');
  const requestId = splits.slice(0, splits.length - 5).join('-');
  const search = props.location.query.search || '';
  const path = props.params.splat;
  const initPromise = props.initialize(requestId, path, search, [props.params.taskId], props.location.query.viewMode || 'split', 'COMPRESSED');
  initPromise.then(() => {
    props.updateActiveTasks(requestId);
  });
}
export const CompressedLogView = connect(null, mapDispatchToProps)(rootComponent(TailPage, getTitle, refreshCompressedLog, false, false));

function refreshAggregateTail(props) {
  const viewMode = props.location.query.viewMode || 'split';
  const search = props.location.query.search || '';
  const taskIds = props.location.query.taskIds;

  let initPromise;
  if (taskIds) {
    initPromise = props.initialize(props.params.requestId, props.params.splat, search, taskIds.split(','), viewMode, 'SANDBOX');
  } else {
    initPromise = props.initializeUsingActiveTasks(props.params.requestId, props.params.splat, search, viewMode, 'SANDBOX');
  }
  initPromise.then(() => {
    props.updateActiveTasks(props.params.requestId);
  });
}
export const AggregateTail = connect(null, mapDispatchToProps)(rootComponent(TailPage, getTitle, refreshAggregateTail, false, false));
