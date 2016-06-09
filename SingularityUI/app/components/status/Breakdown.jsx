import React from 'react';
import Table from '../common/Table';
import PlainText from '../common/atomicDisplayItems/PlainText';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import Timestamp from '../common/atomicDisplayItems/Timestamp';
import Utils from '../../utils';

export default class HostStates extends React.Component {

  renderItems() {
    return this.props.data.map((d, i) => {
      if (!d) {return}
      let ComponentClass = d.component;
      return (
        <li key={i} className="list-group-item"><ComponentClass prop={d.prop} /></li>
      );
    });
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

HostStates.propTypes = {
  data: React.PropTypes.arrayOf(React.PropTypes.shape({
      component: React.PropTypes.func.isRequired,
      prop: React.PropTypes.object,
      id: React.PropTypes.string,
      className: React.PropTypes.string
  })).isRequired
};
