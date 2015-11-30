
Help = React.createClass

  render: ->
    <div>
      <h3>Log View Help</h3>
      <p>
        The log view allows viewing and tailing of files inside the task sandbox.
        The same file can be viewed across up to 6 tasks at time.
      </p>
      <h4><span className="glyphicon glyphicon-search"></span> Grep</h4>
      <p>
        Enter a string here to only display lines in the file that contain a match. Regular expressions are supported.<br/>
        While the input is open, press <kbd>return</kbd> to commit the search. Press <kbd>esc</kbd> to clear the current search.
        The button will turn blue to indicate a search string is currently being applied.
      </p>
      <h4><span className="glyphicon glyphicon-tasks"></span> Select Instances</h4>
      <p>
        This dropdown contains all running instances of the request. Select up to 6 of them to tail the file across.
      </p>
      <h4><span className="glyphicon glyphicon-adjust"></span> Color Scheme</h4>
      <p>
        Set the color scheme used by the file viewer.
      </p>
      <h4><span className="glyphicon glyphicon-list-alt"></span> Unified/Split</h4>
      <p>
        Toggle between Unified and Split views of the log lines.<br/>
        <strong>Split View:</strong> Displays each task in a seperate pane.<br/>
        <strong>Unified View:</strong> Interleaves the log lines, ordered based on timestamps contained in them.
        This assumes Singularity is able to locate and parse timestamps in the file being viewed.
      </p>
      <h4><span className="glyphicon glyphicon-chevron-up"></span><span className="glyphicon glyphicon-chevron-down"></span> Bottom/Top</h4>
      <p>
        Jump to the bottom or top of the file(s) being viewed.
      </p>
    </div>

module.exports = Help
