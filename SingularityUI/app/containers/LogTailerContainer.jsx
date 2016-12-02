import React from 'react';
import classNames from 'classnames';
import { TailerProvider, Pane, SandboxTailer } from 'singularityui-tailer';
import NewTaskGroupHeader from '../components/logs/NewTaskGroupHeader';
import NewHeader from '../components/logs/NewHeader';
import Footer from '../components/logs/Footer';

import { connect } from 'react-redux';

import { setTailerGroups, setColor, loadColor, removeTailerGroup, pickTailerGroup } from '../actions/tailer';

const LogTailerContainer = ({tailerGroups, requestIds, color, removeTailerGroup, pickTailerGroup}) => {
  const renderTailerPane = (tasks, key) => {
    const {taskId, path, offset, tailerId} = tasks[0];

    const header = (<NewTaskGroupHeader
      taskId={taskId}
      showRequestId={requestIds.length > 1}
      showCloseAndExpandButtons={tailerGroups.length > 1}
      onClose={() => removeTailerGroup(key)}
      onExpand={() => pickTailerGroup(key)}
      onJumpToTop={() => console.log("jump to top")}
      onJumpToBottom={() => console.log("jump to bottom")} />);

    const component = (<SandboxTailer
      goToOffset={parseInt(offset)}
      tailerId={tailerId}
      taskId={taskId}
      path={path.replace('$TASK_ID', taskId)}
      hrefFunc={(tailerId, offset) => `${config.appRoot}/task/${taskId}/new-tail/${path}?offset=${offset}`}/>);

    const footer = (<Footer tailerId={tailerId} />);

    return (<Pane key={key} logHeader={header} logComponent={component} logFooter={footer} />);
  };

  return (
    <TailerProvider getTailerState={(state) => state.tailer}>
      <div className={classNames(['new-tailer', 'tail-root', color])}>
        <NewHeader />
        <div className="row tail-row">
          {tailerGroups.map(renderTailerPane)}
        </div>
      </div>
    </TailerProvider>
  );
}

export default connect((state) => ({
  tailerGroups: state.tailerView.tailerGroups,
  requestIds: state.tailerView.requestIds,
  color: state.tailerView.color
}), {
  setTailerGroups,
  setColor,
  loadColor,
  removeTailerGroup,
  pickTailerGroup,
})(LogTailerContainer);
