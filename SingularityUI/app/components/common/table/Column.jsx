import React, { Component, PropTypes } from 'react';

class Column extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.func
    ]),
    cellData: PropTypes.func,
    renderCell: PropTypes.func,
    sortFunc: PropTypes.func,
    className: PropTypes.string,
    headerClassName: PropTypes.string
  };

  static defaultProps = {
    cellData: (o) => o,
    renderCell: (str) => str,
    label: '',
    sortFunc: (a, b) => a > b
  };

  displayName = 'Column';

  constructor(props) {
    super(props);
  }
}

export default Column;
