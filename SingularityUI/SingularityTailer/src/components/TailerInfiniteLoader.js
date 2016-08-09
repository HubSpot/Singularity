import { Component, PropTypes } from 'react';
import { Range } from 'immutable';

class TailerInfiniteLoader extends Component {
  constructor (props, context) {
    super(props, context);

    this._onRowsRendered = this._onRowsRendered.bind(this);

    this.state = {
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
    return range.filter((index) => !this.props.isLineLoaded({ index }));
  }

  _onRowsRendered ({ startIndex, stopIndex, overscanStartIndex, overscanStopIndex }) {
    const { useOverscan } = this.props;
    this.setState({
      startIndex: useOverscan ? overscanStartIndex : startIndex,
      stopIndex: useOverscan ? overscanStopIndex : stopIndex
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
  loadLines: PropTypes.func.isRequired,
  useOverscan: PropTypes.bool,
};

export default TailerInfiniteLoader;
