import React from 'react';
import classNames from 'classnames';
import { TailerProvider, Pane, SandboxTailer } from 'singularityui-tailer';
import NewTaskGroupHeader from '../components/logs/NewTaskGroupHeader';
import NewHeader from '../components/logs/NewHeader';
import Footer from '../components/logs/Footer';
import LogActions from '../actions/log';

import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

import { setTailerGroups } from '../actions/tailer';

class LogTailerContainer extends React.Component {
  renderTailerPane(tasks, key) {
    const {taskId, path, offset, tailerId} = tasks[0];
    const header = (<NewTaskGroupHeader tailerId={tailerId} taskId={taskId} path={path} />);
    const component = (<SandboxTailer goToOffset={offset} tailerId={tailerId} taskId={taskId} path={path.replace('$TASK_ID', taskId)} />);
    const footer = (<Footer tailerId={tailerId} />);
    return (<Pane key={key} logHeader={header} logComponent={component} logFooter={footer} />);
  }

  render() {
    return (
      <TailerProvider getTailerState={(state) => state.tailer}>
        <div className={classNames(['new-tailer', 'tail-root', this.props.color])}>
          <NewHeader />
          <div className="row tail-row">
            {this.props.tailerGroups.map(this.renderTailerPane)}
          </div>
        </div>
      </TailerProvider>
    );
  }
};

export default connect((state) => ({
  tailerGroups: state.tailerView.tailerGroups,
  color: state.activeColor
}), {
  setTailerGroups
})(LogTailerContainer);
