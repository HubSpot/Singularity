import React from 'react';
import Table from '../common/Table';
import PlainText from '../common/atomicDisplayItems/PlainText';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import Timestamp from '../common/atomicDisplayItems/Timestamp';
import Utils from '../../utils';

export default class StatusList extends React.Component {

  constructor() {
    super();
    this.state = {
      changes: []
    };
  }

  componentWillReceiveProps(nextProps) {
    let changes = [];
    for (let d of nextProps.data) {
      if (!d || !d.prop.id) return;
      let matchingData = _.find(this.props.data, (data) => {
        if (data.prop.id == d.prop.id) {
          return true;
        }
      });
      if (d.prop.value && matchingData && d.prop.value != matchingData.prop.value) {
        changes.push({
          id: d.prop.id,
          diff: d.prop.value - matchingData.prop.value
        });
      }
    }
    this.setState({
      changes: changes
    });
  }

  renderBefore(d) {
    if (d.beforeFill) {
      return (
        <span className={`chart__legend-fill chart-fill-${d.beforeFill}`}></span>
      );
    }
  }

  renderItems() {
    return this.props.data.map((d, i) => {
      if (!d) {return}
      let ComponentClass = d.component;
      let diff = this.getDiffFor(d)
      let cn = {};
      if (diff) {
        cn = {className: diff.diff > 0 ? 'changed-direction-increase' : 'changed-direction-decrease'};
      }

      return (
        <li key={i} className="list-group-item">
          {this.renderBefore(d)}
          <ComponentClass prop={_.extend(d.prop, cn)} />
          {this.renderDiff(diff)}
        </li>
      );
    });
  }

  getDiffFor(d) {
    if (!d.prop.id) return;
    return _.find(this.state.changes, (c) => { return c.id == d.prop.id});
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
