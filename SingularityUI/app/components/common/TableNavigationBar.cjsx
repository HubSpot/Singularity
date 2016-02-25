Glyphicon = require './atomicDisplayItems/Glyphicon'
IconButton = require './atomicDisplayItems/IconButton'
FormField = require './formItems/FormField'
DropDown = require './formItems/DropDown'

TableNavigationBar = React.createClass

    getInitialState: ->
        {}

    handlePageJump: (event) ->
        event.preventDefault()
        if @state.pageNumberEntered >= 1 and (not @props.numberOfPages or @state.pageNumberEntered <= @props.numberOfPages)
            @props.setPageNumber(@state.pageNumberEntered)
        @setState({
            pageNumberEntered: ''
        })

    createSetPageNumberFn: (number, setPageNumberFn) ->
        -> setPageNumberFn number

    updatePageNumber: (event) ->
        @setState({
            pageNumberEntered: parseInt(event.target.value)
        })

    updateNumberPerPage: (event) ->
        @props.setNumberPerPage parseInt(event.target.value)

    pageNumbersToDisplay: ->
        pagesAround = @props.numberOfPagesAroundCurrentPage and @props.numberOfPagesAroundCurrentPage or 0
        lowerBound = @props.currentPage - pagesAround
        lowerBound = 1 if lowerBound < 1
        upperBound = parseInt(@props.currentPage, 10) + parseInt(pagesAround)
        upperBound = @props.numberOfPages if @props.numberOfPages and upperBound > @props.numberOfPages
        toDisplay = [lowerBound..upperBound]
        toDisplay.unshift 1 if lowerBound > 1 # Always allow jump to page 1
        toDisplay.push @props.numberOfPages if @props.numberOfPages and @props.numberOfPages > upperBound # Always allow jump to last page
        return toDisplay

    renderPageNumber: (pageNumber, key) ->
        <li
        aria-label = pageNumber
        onClick = {@createSetPageNumberFn pageNumber, @props.setPageNumber}
        key = key>
            <button className = {classNames {
                'btn-primary': pageNumber == @props.currentPage
                'btn-default': pageNumber != @props.currentPage
                'btn': true
            }}>{pageNumber}</button>
        </li>


    renderPageSelectors: ->
        <div>
            <li 
            aria-label = 'Previous'
            onClick = @props.decreasePageNumber>
                <IconButton
                    prop = {{
                        iconClass: 'chevron-left'
                        btnClass: 'default'
                        className: classNames {
                            'disabled': @props.currentPage == 1
                        }
                }}/>
            </li>
            {@pageNumbersToDisplay().map @renderPageNumber}
            <li
            aria-label = 'Next'
            onClick = @props.increasePageNumber>
                <IconButton
                    prop = {{
                        iconClass: 'chevron-right'
                        btnClass: 'default'
                        className: classNames {
                            disabled: @props.numberOfPages and @props.currentPage == @props.numberOfPages
                        }
                }}/>
            </li>
        </div>

    renderJumpToPage: ->
        <form role='form' onSubmit={@handlePageJump} className='form-inline'>
            <label htmlFor="pageNumber" className="sr-only">Jump To Page:</label>
            <FormField 
                id = 'pageNumber'
                prop = {{
                    value: @state.pageNumberEntered 
                    inputType: 'number'
                    updateFn: @updatePageNumber
                    placeholder: 'Jump to Page'
                    min: 1
                    max: @props.numberOfPages if @props.numberOfPages
                }} />
            <button type="submit" className="btn btn-default">Jump!</button>
        </form>

    renderNumberPerPage: ->
        <form role='form' className = 'form-inline'>
            <label htmlFor='count'>{@props.objectsBeingDisplayed} Per Page</label>
            <DropDown
                id = 'count'
                prop = {{
                    forceChooseValue: true
                    value: @props.numberPerPage
                    choices: @props.numberPerPageChoices
                    inputType: 'number'
                    updateFn: @updateNumberPerPage
                }} />
        </form>

    renderSortDirection: ->
        <form role='form' className = 'form-inline'>
            <label htmlFor='sortDirection'>Sort Direction</label>
            <DropDown
                id = 'sortDirection'
                prop = {{
                    forceChooseValue: true
                    value: @props.sortDirection
                    choices: @props.sortDirectionChoices
                    inputType: 'text'
                    updateFn: @props.setSortDirection
                }} />
        </form>
    
    ###Props:
        - currentPage number
        - increasePageNumber function
        - decreasePageNumber function
        - setPageNumber function (will take a number, not an event or anything else)
        - showJumpToPage boolean
        - numberOfPages number (must be optional b/c taskSearch doesn't have it yet)
        - numberOfPagesAroundCurrentPage number (optional - defaults to 0, 
                but page 1 is always shown and the last page is shown if known)
        - numberPerPage number (optional)
        - objectsBeingDisplayed string (optional; for now only used if numberPerPage is provided)
        - numberPerPageChoices Array/Enum object (optional; only used if numberPerPage is provided)
        - setNumberPerPage function (optional; only used if numberPerPage is provided)
        - sortDirection SortDirection (optional)
        - sortDirectionChoices Array/Enum object (optional; only used if sortDirection is provided. 
                Needed becasue each project may represent sort direction differently internally)
        - setSortDirection function (optional; only used if sortOrder is provided)
    ###
    render: ->
        <div>
            <ul className = 'pager table-nav-bar'>
                <div className = 'pull-left roomy-left roomy-right'>
                    {@renderPageSelectors()}
                </div>
                <div className = 'pull-left roomy-left roomy-right'>
                    {@renderJumpToPage()}
                </div>
                <div className = 'pull-left roomy-left roomy-right'>
                    {@renderNumberPerPage() if @props.numberPerPage}
                </div>
                <div className = 'pull-left roomy-left roomy-right'>
                    {@renderSortDirection() if @props.sortDirection}
                </div>
            </ul>
        </div>
        

module.exports = TableNavigationBar
