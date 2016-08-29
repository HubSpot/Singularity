import { Component, PropTypes } from 'react';

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
    sortData: PropTypes.func, // (cellData, object) -> any
    sortFunc: PropTypes.func,
    className: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.func
    ]),
    headerClassName: PropTypes.string
  };

  static defaultProps = {
    cellData: (rowData) => rowData,
    cellRender: (cellData) => cellData,
    label: '',
    sortable: false,
    sortData: (cellData) => cellData,
    /* eslint-disable id-length */ // Exception for comparator
    sortFunc: (a, b) => {
      /* eslint-enable id-length */
      if (a < b) {
        return -1;
      }
      if (a > b) {
        return 1;
      }
      return 0;
    }
  };
}

export default Column;
