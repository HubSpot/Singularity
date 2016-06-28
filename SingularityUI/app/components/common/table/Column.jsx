import React, { Component, PropTypes } from 'react';

class Column extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired, // must be unique
    label: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.func
    ]),
    cellData: PropTypes.func,
    cellRender: PropTypes.func,
    sortable: PropTypes.bool,
    sortData: PropTypes.func,
    sortFunc: PropTypes.func,
    className: PropTypes.string,
    headerClassName: PropTypes.string
  };

  static defaultProps = {
    cellData: (rowData) => rowData,
    cellRender: (cellData, rowData) => cellData,
    label: '',
    sortable: false,
    sortData: (cellData, rowData) => cellData,
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

  constructor(props) {
    super(props);
  }
}

export default Column;
