import React from 'react';

export default class StatusList extends React.Component {

  constructor() {
    super();
    this.state = {
      changes: []
    };
  }

  componentWillReceiveProps(nextProps) {
    const changes = [];
    for (const nextDataItem of nextProps.data) {
      if (!nextDataItem || !nextDataItem.id) continue;
      const matchingData = _.find(this.props.data, (currentDataItem) => (currentDataItem.id === nextDataItem.id));
      if (nextDataItem.value && matchingData && nextDataItem.value !== matchingData.value) {
        changes.push({
          id: nextDataItem.id,
          diff: nextDataItem.value - matchingData.value
        });
      }
    }
    this.setState({ changes });
  }

  getDiffFor(diff) {
    if (!diff.id) return null;
    return _.find(this.state.changes, (change) => { return change.id === diff.id; });
  }

  renderItems() {
    return this.props.data.map((dataItem, key) => {
      if (!dataItem) { return null; }
      const diff = this.getDiffFor(dataItem);
      let className = '';
      if (diff) {
        className = diff.diff > 0 ? 'changed-direction-increase' : 'changed-direction-decrease';
      }

      return (
        <li key={key} className="list-group-item">
          {this.renderBefore(dataItem)}
          {dataItem.component(className)}
          {this.renderDiff(diff)}
        </li>
      );
    });
  }

  renderBefore(dataItem) {
    return dataItem.beforeFill && (
      <span className={`chart__legend-fill bg-${dataItem.beforeFill}`}></span>
    );
  }

  renderDiff(change) {
    if (change) {
      setTimeout(() => {
        this.setState({changes: _.without(this.state.changes, change)});
      }, 4000);
      return (
        <span className={`changeDifference ${change.diff > 0 ? 'color-success' : 'color-warning'}`}>{`${change.diff > 0 ? '+' : ''}${change.diff}`}</span>
      );
    }
    return null;
  }

  render() {
    return (
      <div>
        <h2>{this.props.header}</h2>
        <ul className="list-group list-group--links list-group--plain list-group--slim list-large">{this.renderItems()}</ul>
      </div>
    );
  }
}

StatusList.propTypes = {
  header: React.PropTypes.string,
  data: React.PropTypes.arrayOf(React.PropTypes.shape({
    component: React.PropTypes.func.isRequired, // className -> node
    value: React.PropTypes.number,
    id: React.PropTypes.string,
    className: React.PropTypes.string,
    beforeFill: React.PropTypes.string
  })).isRequired
};
