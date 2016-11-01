import React from 'react';
import { TailerProvider, Pane, SandboxTailer } from 'singularityui-tailer';
import TaskGroupHeader from '../components/logs/TaskGroupHeader';
import Header from '../components/logs/Header';

import { connect } from 'react-redux';

class LogTailerContainer extends React.Component {
  renderTailerPane(taskId, path, key) {
    const header = (<TaskGroupHeader />);
    const component = (<SandboxTailer tailerId={`${key}-${taskId}/${path}`} taskId={taskId} path={path} />);
    const footer = (<div>footer</div>);
    return (<Pane key={key} logHeader={header} logComponent={component} logFooter={footer} />);
  }

  renderTailerRow(taskIds, path, key) {
    return (<div className="row tail-row" key={key}>
      {taskIds.map((taskId, key) => this.renderTailerPane(taskId, path, key))}
    </div>);
  }

  render() {
    return (
      <TailerProvider getTailerState={(state) => state.tailer}>
        <div className="new-tailer tail-root">
          <Header />
          {this.props.taskIds.map((taskIds, key) => this.renderTailerRow(taskIds, this.props.path, key))}
        </div>
      </TailerProvider>
    );
  }
};


export default connect((state, ownProps) => ({
  taskIds: [[ownProps.params.taskId]],
  path: ownProps.params.splat
}))(LogTailerContainer);
