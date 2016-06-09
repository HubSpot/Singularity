import React from 'react';
import Table from '../common/Table';
import PlainText from '../common/atomicDisplayItems/PlainText';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import Timestamp from '../common/atomicDisplayItems/Timestamp';
import Utils from '../../utils';

export default class StatusList extends React.Component {

  render() {
    return (
      <div className="chart__column">

      </div>
    );
  }
}

StatusList.propTypes = {
  total: React.PropTypes.number.isRequired,
  data: React.PropTypes.arrayOf(React.PropTypes.shape({
    count: React.PropTypes.number.isRequired,
    type: React.PropTypes.string.isRequired,
    label: React.PropTypes.string.isRequired,
    link: React.PropTypes.string.isRequired
  })).isRequired
};
