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
      if (!nextDataItem || !nextDataItem.prop.id) continue;
      const matchingData = _.find(this.props.data, (currentDataItem) => (currentDataItem.prop.id === nextDataItem.prop.id));
      if (nextDataItem.prop.value && matchingData && nextDataItem.prop.value !== matchingData.prop.value) {
        changes.push({
          id: nextDataItem.prop.id,
          diff: nextDataItem.prop.value - matchingData.prop.value
        });
      }
    }
    this.setState({ changes });
  }

  getDiffFor(diff) {
    if (!diff.prop.id) return null;
    return _.find(this.state.changes, (change) => { return change.id === diff.prop.id; });
  }

  renderItems() {
    return this.props.data.map((dataItem, key) => {
      if (!dataItem) { return null; }
      let ComponentClass = dataItem.component;
      const diff = this.getDiffFor(dataItem);
      let className = {className: ''};
      if (diff) {
        className = {className: diff.diff > 0 ? 'changed-direction-increase' : 'changed-direction-decrease'};
      }

      return (
        <li key={key} className="list-group-item">
          {this.renderBefore(dataItem)}
          <ComponentClass prop={_.extend(dataItem.prop, className)} />
          {this.renderDiff(diff)}
        </li>
      );
    });
  }

  renderBefore(d) {
    return d.beforeFill && (
      <span className={`chart__legend-fill chart-fill-${d.beforeFill}`}></span>
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
    component: React.PropTypes.func.isRequired,
    value: React.PropTypes.number,
    prop: React.PropTypes.object,
    id: React.PropTypes.string,
    className: React.PropTypes.string,
    beforeFill: React.PropTypes.string
  })).isRequired
};
