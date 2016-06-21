import React, { Component, PropTypes } from 'react';

class Column extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.func
    ]),
    cellData: PropTypes.func,
    cellRender: PropTypes.func,
    sortable: PropTypes.bool,
    sortFunc: PropTypes.func,
    className: PropTypes.string,
    headerClassName: PropTypes.string
  };

  static defaultProps = {
    cellData: (o, rowData) => o,
    cellRender: (str) => str,
    label: '',
    sortable: false,
    sortFunc: (a, b) => {
      if (a < b) {
        return -1;
      }
      if (a > b) {
        return 1;
      }
      return 0;
    }
  };

  displayName = 'Column';

  constructor(props) {
    super(props);
  }
}

export default Column;
