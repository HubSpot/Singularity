import { Component, PropTypes } from 'react';
import { Range } from 'immutable';

class TailerInfiniteLoader extends Component {
  constructor (props, context) {
    super(props, context);

    this._onRowsRendered = this._onRowsRendered.bind(this);

    this.state = {
      isTailing: false,
      tailIntervalId: undefined
    };
  }

  componentDidUpdate() {
    if (this.state.hasOwnProperty('startIndex')) {
      const unloaded = this.findUnloadedInRange(
        this.state.startIndex,
        this.state.stopIndex
      );

      unloaded.forEach((index) => {
        this.props.loadLines(index, index);
      });
    }
  }

  findUnloadedInRange(startIndex, stopIndex) {
    const range = new Range(startIndex, stopIndex + 1);
    return range.filter((index) => !this.props.isLineLoaded(index));
  }

  _onRowsRendered ({ startIndex, stopIndex, overscanStartIndex, overscanStopIndex }) {
    const { useOverscan } = this.props;

    const isTailing = this.props.isTailing(stopIndex);

    let tailIntervalId = this.state.tailIntervalId;

    if (isTailing && !this.state.isTailing) {
      // start tailing
      tailIntervalId = setInterval(() => this.props.tailLog(), 1000);
    } else if (!isTailing && this.state.isTailing) {
      // stop tailing
      clearInterval(tailIntervalId);
      tailIntervalId = undefined;
    }

    this.setState({
      startIndex: useOverscan ? overscanStartIndex : startIndex,
      stopIndex: useOverscan ? overscanStopIndex : stopIndex,
      isTailing,
      tailIntervalId
    });
  }

  render() {
    const { children } = this.props;

    return children({
      onRowsRendered: this._onRowsRendered,
    });
  }
}

TailerInfiniteLoader.propTypes = {
  /**
   * Function responsible for rendering a virtualized component.
   * This function should implement the following signature:
   * ({ onRowsRendered }) => PropTypes.element
   *
   * The specified :onRowsRendered function should be passed through to the child's :onRowsRendered property.
   */
  children: PropTypes.func.isRequired,

  /**
   * Function responsible for tracking the loaded state of each row.
   * It should implement the following signature: ({ index: number }): boolean
   */
  isLineLoaded: PropTypes.func.isRequired,
  isTailing: PropTypes.func.isRequired,
  loadLines: PropTypes.func.isRequired,
  tailLog: PropTypes.func.isRequired,
  scrollToIndex: PropTypes.func.isRequired,
  useOverscan: PropTypes.bool
};

export default TailerInfiniteLoader;
