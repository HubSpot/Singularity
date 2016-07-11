import React from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import LogContainer from './LogContainer';

import LogActions from '../../actions/log';
import { updateActiveTasks } from '../../actions/activeTasks';

const Tail = () => {
  return <LogContainer />;
};

const mapDispatchToProps = {
  updateActiveTasks,
  initialize: LogActions.initialize,
  initializeUsingActiveTasks: LogActions.initializeUsingActiveTasks
};

function refresh(props) {
  console.log(props);
  const splits = props.params.taskId.split('-');
  const requestId = splits.slice(0, splits.length - 5).join('-');
  const search = props.location.query.search || '';
  const path = props.params.splat.replace(props.params.taskId, '$TASK_ID');
  // const requestId =
  const initPromise = props.initialize(requestId, path, search, [props.params.taskId], props.location.query.viewMode || 'split');
  initPromise.then(() => {
    props.updateActiveTasks(props.params.requestId);
  });
}

export default connect(null, mapDispatchToProps)(rootComponent(Tail, 'Tail of', refresh, false));
