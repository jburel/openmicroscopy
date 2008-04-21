function handles = Tile(handles)

% Help for the Tile module:
% Category: Image Processing
%
% SHORT DESCRIPTION:
% Creates one large, tiled image from all images of a certain type.
% *************************************************************************
%
% Allows many images to be viewed simultaneously, in a grid layout you
% specify (e.g. in the actual layout in which the images were collected).
%
% If you want to view a large number of images, you will generate an
% extremely large file (roughly the size of all the images' sizes added
% together) which, even if it could be created, could not be opened by any
% image software anyway. There are several ways to allow a larger image to
% be produced, given memory limitations: (1) Decrease the resolution of
% each image tile by entering a fraction where requested. Then, in the
% window which pops open after Tile finishes, you can use the 'Get high res
% image' button to retrieve the original high resolution image. (Sorry,
% this button is not yet functional). (2) Use the SpeedUpCellProfiler
% module just before this module to clear out images that are stored in
% memory. Place this module just prior to the Tile module (and maybe also
% afterwards) and ask it to retain only those images which are needed for
% downstream modules. (3) Rescale the images to 8 bit format by putting in
% the RescaleIntensity module just prior to the Tile module. Normally
% images are stored in memory as class "double" which takes about 10 times
% the space of class "uint8" which is 8 bits. You will lose resolution in
% terms of the number of different graylevels - this will be limited to 256
% - but you will not lose spatial resolution.
%
% The file name (automatic) and sample info (optional) can be displayed on
% each image using buttons in the final figure window.
%
% See also PlaceAdjacent.

% CellProfiler is distributed under the GNU General Public License.
% See the accompanying file LICENSE for details.
%
% Developed by the Whitehead Institute for Biomedical Research.
% Copyright 2003,2004,2005.
%
% Authors:
%   Anne E. Carpenter
%   Thouis Ray Jones
%   In Han Kang
%   Ola Friman
%   Steve Lowe
%   Joo Han Chang
%   Colin Clarke
%   Mike Lamprecht
%   Peter Swire
%   Rodrigo Ipince
%   Vicky Lay
%   Jun Liu
%   Chris Gang
%
% Website: http://www.cellprofiler.org
%
% $Revision: 4879 $

%%%%%%%%%%%%%%%%%
%%% VARIABLES %%%
%%%%%%%%%%%%%%%%%
drawnow

[CurrentModule, CurrentModuleNum, ModuleName] = CPwhichmodule(handles);

%textVAR01 = What did you call the images to be tiled?
%infotypeVAR01 = imagegroup
ImageName = char(handles.Settings.VariableValues{CurrentModuleNum,1});
%inputtypeVAR01 = popupmenu

%textVAR02 = What were the images called when they were originally loaded?
%infotypeVAR02 = imagegroup
OrigImageName = char(handles.Settings.VariableValues{CurrentModuleNum,2});
%inputtypeVAR02 = popupmenu

%textVAR03 = What do you want to call the tiled image?
%defaultVAR03 = TiledImage
%infotypeVAR03 = imagegroup indep
TiledImageName = char(handles.Settings.VariableValues{CurrentModuleNum,3});

%textVAR04 = Number of rows to display
%choiceVAR04 = Automatic
NumberRows = char(handles.Settings.VariableValues{CurrentModuleNum,4});
%inputtypeVAR04 = popupmenu custom

%textVAR05 = Number of columns to display
%choiceVAR05 = Automatic
NumberColumns = char(handles.Settings.VariableValues{CurrentModuleNum,5});
%inputtypeVAR05 = popupmenu custom

%textVAR06 = Are the first two images arranged in a row or a column?
%choiceVAR06 = Column
%choiceVAR06 = Row
RowOrColumn = char(handles.Settings.VariableValues{CurrentModuleNum,6});
%inputtypeVAR06 = popupmenu

%textVAR07 = Is the first image at the bottom or the top?
%choiceVAR07 = Top
%choiceVAR07 = Bottom
TopOrBottom = char(handles.Settings.VariableValues{CurrentModuleNum,7});
%inputtypeVAR07 = popupmenu

%textVAR08 = Is the first image at the left or the right?
%choiceVAR08 = Left
%choiceVAR08 = Right
LeftOrRight = char(handles.Settings.VariableValues{CurrentModuleNum,8});
%inputtypeVAR08 = popupmenu

%textVAR09 = Would you like to go in tile them in meander mode?
%choiceVAR09 = No
%choiceVAR09 = Yes
MeanderMode = char(handles.Settings.VariableValues{CurrentModuleNum,9});
%inputtypeVAR09 = popupmenu

%textVAR10 = What fraction should the images be sized (the resolution will be changed)?
%defaultVAR10 = .1
SizeChange = char(handles.Settings.VariableValues{CurrentModuleNum,10});
SizeChange = str2double(SizeChange);

%%%VariableRevisionNumber = 1

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% PRELIMINARY CALCULATIONS & FILE HANDLING %%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
drawnow

%%% Reads (opens) the image you want to analyze and assigns it to a
%%% variable.

%%% OK to not use CPretrieveimage here, because we only want to check that
%%% the images have been loaded; we do not need to retrieve them yet.
if ~isfield(handles.Pipeline, ImageName)
    %%% If the image is not there, an error message is produced.  The error
    %%% is not displayed: The error function halts the current function and
    %%% returns control to the calling function (the analyze all images
    %%% button callback.)  That callback recognizes that an error was
    %%% produced because of its try/catch loop and breaks out of the image
    %%% analysis loop without attempting further modules.
    error(['Image processing was canceled in the ', ModuleName, ' module because CellProfiler could not find the input image. CellProfiler expected to find an image named "', ImageName, '", but that image has not been created by the pipeline. Please adjust your pipeline to produce the image "', ImageName, '" prior to this ', ModuleName, ' module.'])
end

%%%%%%%%%%%%%%%%%%%%%%
%%% IMAGE ANALYSIS %%%
%%%%%%%%%%%%%%%%%%%%%%
drawnow

if handles.Current.SetBeingAnalyzed == 1
    %%% Retrieves the path where the images are stored from the handles
    %%% structure.
    fieldname = ['Pathname', OrigImageName];
    try Pathname = handles.Pipeline.(fieldname); %#ok Ignore MLint
    catch error(['Image processing was canceled in the ', ModuleName, ' module because it must be run using images straight from a Load Images module (i.e. the images cannot have been altered by other image processing modules). This is because this module needs all of the images before tiling them. One solution is to process the entire batch of images using the image analysis modules preceding this module and save the resulting images to the hard drive, then start a new stage of processing from this module onward.'])
    end
    %%% Retrieves the list of filenames where the images are stored from the
    %%% handles structure.
    fieldname = ['FileList', OrigImageName];
    FileList = handles.Pipeline.(fieldname);
    NumberOfImages = length(FileList);
    if strcmp(NumberRows,'Automatic') && strcmp(NumberColumns,'Automatic')
        %%% Calculates the square root in order to determine the dimensions
        %%% of the display grid.
        SquareRoot = sqrt(NumberOfImages);
        %%% Converts the result to an integer.
        NumberRows = fix(SquareRoot);
        NumberColumns = ceil((NumberOfImages)/NumberRows);
    elseif strcmp(NumberRows,'Automatic')
        NumberColumns = str2double(NumberColumns);
        NumberRows = ceil((NumberOfImages)/NumberColumns);
    elseif strcmp(NumberColumns,'Automatic')
        NumberRows = str2double(NumberRows);
        NumberColumns = ceil((NumberOfImages)/NumberRows);
    else NumberColumns = str2double(NumberColumns);
        NumberRows = str2double(NumberRows);
    end
    if NumberRows*NumberColumns > NumberOfImages;
        Answer = CPquestdlg(['You have specified ', num2str(NumberRows), ' rows and ', num2str(NumberColumns), ' columns (=',num2str(NumberRows*NumberColumns),' images), but there are ', num2str(length(FileList)), ' images loaded. The image locations at the end of the grid for which there is no image data will be displayed as black. Do you want to continue?'],'Continue?','Yes','No','Yes');
        if strcmp(Answer,'No') == 1
            return
        end
        FileList(length(FileList)+1:NumberRows*NumberColumns) = {'none'};
    elseif NumberRows*NumberColumns < NumberOfImages;
        Answer = CPquestdlg(['You have specified ', num2str(NumberRows), ' rows and ', num2str(NumberColumns), ' columns (=',num2str(NumberRows*NumberColumns),' images), but there are ', num2str(length(FileList)), ' images loaded. Images at the end of the list will not be displayed. Do you want to continue?'],'Continue?','Yes','No','Yes');
        if strcmp(Answer,'No') == 1
            return
        end
        FileList(NumberRows*NumberColumns+1:NumberOfImages) = [];
    end

    if strcmp(RowOrColumn,'Row')
        if strcmp(MeanderMode,'Yes')
            for (i=[2:2:NumberRows])
                FileList((i-1)*NumberColumns+1:i*NumberColumns) = FileList(i*NumberColumns:-1:(i-1)*NumberColumns+1);
            end
        end
        NewFileList = reshape(FileList,NumberColumns,NumberRows);
        NewFileList = NewFileList';
    elseif strcmp(RowOrColumn,'Column')
        if strcmp(MeanderMode,'Yes')
            for (i=[2:2:NumberColumns])
                FileList((i-1)*NumberRows+1:i*NumberRows) = FileList(i*NumberRows:-1:(i-1)*NumberRows+1);
            end
        end
        NewFileList = reshape(FileList,NumberRows,NumberColumns);
    end
    if strcmp(LeftOrRight,'Right')
        NewFileList = fliplr(NewFileList);
    end
    if strcmp(TopOrBottom,'Bottom')
        NewFileList = flipud(NewFileList);
    end

    LoadedImage = handles.Pipeline.(ImageName);
    ImageSize = size(imresize(LoadedImage,SizeChange));
    ImageHeight = ImageSize(1);
    ImageWidth = ImageSize(2);
    TotalWidth = NumberColumns*ImageWidth;
    TotalHeight = NumberRows*ImageHeight;
    %%% Preallocates the array to improve speed. The data class for
    %%% the tiled image is set to match the incoming image's class.
    TiledImage = zeros(TotalHeight,TotalWidth,size(LoadedImage,3),class(LoadedImage));

    TileDataToSave.NumberColumns = NumberColumns;
    TileDataToSave.NumberRows = NumberRows;
    TileDataToSave.ImageHeight = ImageHeight;
    TileDataToSave.ImageWidth = ImageWidth;
    TileDataToSave.NewFileList = NewFileList;
    TileDataToSave.TotalWidth = TotalWidth;
    TileDataToSave.TotalHeight = TotalHeight;
    TileDataToSave.TiledImage = TiledImage;

    %stores data in handles
    handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]) = TileDataToSave;
end

%gets data from handles
ImageHeight = handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).ImageHeight;
ImageWidth = handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).ImageWidth;
NumberColumns = handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).NumberColumns;
NumberRows = handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).NumberRows;
NewFileList = handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).NewFileList;

CurrentImage = handles.Pipeline.(ImageName);
if SizeChange ~= 1
    CurrentImage = imresize(CurrentImage,SizeChange);
end

if strcmp(RowOrColumn,'Column')
    HorzPos = floor((handles.Current.SetBeingAnalyzed-1)/NumberRows);
    VertPos = handles.Current.SetBeingAnalyzed - HorzPos*NumberRows-1;
    if strcmp(MeanderMode,'Yes') && mod(HorzPos,2)==1
        VertPos = NumberRows - VertPos - 1;
    end
elseif strcmp(RowOrColumn,'Row')
    VertPos = floor((handles.Current.SetBeingAnalyzed-1)/NumberColumns);
    HorzPos = handles.Current.SetBeingAnalyzed - VertPos*NumberColumns-1;
    if strcmp(MeanderMode,'Yes') && mod(VertPos,2)==1
        HorzPos = NumberColumns - HorzPos-1;
    end
end

if strcmp(TopOrBottom,'Bottom')
    VertPos = NumberRows - VertPos-1;
end

if strcmp(LeftOrRight,'Right')
    HorzPos = NumberColumns - HorzPos-1;
end


%%% Memory errors can occur here if the tiled image is too big.
handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).TiledImage((ImageHeight*VertPos)+(1:ImageHeight),(ImageWidth*HorzPos)+(1:ImageWidth),:) = CurrentImage(:,:,:);

if handles.Current.SetBeingAnalyzed == handles.Current.NumberOfImageSets

    %%%%%%%%%%%%%%%%%%%%%%%
    %%% DISPLAY RESULTS %%%
    %%%%%%%%%%%%%%%%%%%%%%%
    drawnow

    %gets data from handles
    TotalWidth = handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).TotalWidth;
    TotalHeight = handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).TotalHeight;
    NewFileList = handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).NewFileList;
    ThisModuleFigureNumber = handles.Current.(['FigureNumberForModule',CurrentModule]);
    if any(findobj == ThisModuleFigureNumber)
        %%% Activates the appropriate figure window.
        CPfigure(handles,'Image',ThisModuleFigureNumber);
        if handles.Current.SetBeingAnalyzed == handles.Current.StartingImageSet
            CPresizefigure(handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).TiledImage,'OneByOne',ThisModuleFigureNumber)
        end
        %%% Displays the image.
        CPimagesc(handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).TiledImage,handles);
        title('Tiled image')

        FontSize = handles.Preferences.FontSize;
        ToggleGridButtonFunction = ...
            ['Handles = findobj(''type'',''line'');'...
            'button = findobj(''Tag'',''ToggleGridButton'');'...
            'if strcmp(get(button,''String''),''Hide''),'...
            'set(button,''String'',''Show'');'...
            'set(Handles,''visible'',''off'');'...
            'else,'...
            'set(button,''String'',''Hide'');'...
            'set(Handles,''visible'',''on'');'...
            'end,'...
            'clear Handles button'];
        uicontrol('Style', 'pushbutton', ...
            'String', 'Hide', 'Position', [10 6 45 20], 'BackgroundColor',[.7 .7 .9],...
            'Callback', ToggleGridButtonFunction, 'parent',ThisModuleFigureNumber,'FontSize',FontSize,'Tag','ToggleGridButton');

        if ~isdeployed
            ChangeGridButtonFunction = 'Handles = findobj(''type'',''line'');  VersionCheck = version; if ispc || str2num(VersionCheck(1:3)) >= 7.1, propedit(Handles); else, CPwarndlg(''Property Editor does not work on this version of Mac.'');end,clear Handles';
            uicontrol('Style', 'pushbutton', ...
                'String', 'Change', 'Position', [60 6 45 20],'BackgroundColor',[.7 .7 .9], ...
                'Callback', ChangeGridButtonFunction, 'parent',ThisModuleFigureNumber,'FontSize',FontSize);
        end

        ToggleFileNamesButtonFunction = ...
            ['Handles = findobj(''UserData'',''FileNameTextHandles'');'...
            'button = findobj(''Tag'',''ToggleFileNamesButton'');'...
            'if strcmp(get(button,''String''),''Hide''),'...
            'set(button,''String'',''Show'');'...
            'set(Handles,''visible'',''off'');'...
            'else,'...
            'set(button,''String'',''Hide'');'...
            'set(Handles,''visible'',''on'');'...
            'end,'...
            'clear Handles button'];
        uicontrol('Style', 'pushbutton', ...
            'String', 'Show', 'Position', [120 6 45 20], 'BackgroundColor',[.7 .7 .9],...
            'Callback', ToggleFileNamesButtonFunction, 'parent',ThisModuleFigureNumber,'FontSize',FontSize,'Tag','ToggleFileNamesButton');

        if ~isdeployed
            ChangeFileNamesButtonFunction = 'Handles = findobj(''UserData'',''FileNameTextHandles''); VersionCheck = version; if ispc || str2num(VersionCheck(1:3)) >= 7.1, propedit(Handles); else, CPwarndlg(''Property Editor does not work on this version of Mac.'');end,clear Handles';
            uicontrol('Style', 'pushbutton', 'BackgroundColor',[.7 .7 .9],...
                'String', 'Change', 'Position', [170 6 45 20], ...
                'Callback', ChangeFileNamesButtonFunction, 'parent',ThisModuleFigureNumber,'FontSize',FontSize);

            ChangeColormapButtonFunction = 'ImageHandle = findobj(gca, ''type'',''image''); VersionCheck = version; if ispc || str2num(VersionCheck(1:3)) >= 7.1, propedit(ImageHandle); else, CPwarndlg(''Property Editor does not work on this version of Mac.'');end,clear ImageHandle';
            uicontrol('Style', 'pushbutton', ...
                'String', 'Change', 'Position', [230 6 45 20], 'BackgroundColor',[.7 .7 .9],...
                'Callback', ChangeColormapButtonFunction, 'parent',ThisModuleFigureNumber,'FontSize',FontSize);
        end

        HiResFunction = ['Oldtitle = get(get(gca,''title''),''string'');'...
            'title(''HI-RES mode: click on at least two points surrounding the area you would like to view''),'...
            '[Xcord,Ycord] = getpts;'...
            'title(Oldtitle);'...
            'if (length(Xcord) < 2) || (length(Ycord) < 2),'...
            '   CPerrordlg(''You must click at least two points in the image and press enter (Tile module).'');'...
            '   [Xcord,Ycord] = getpts;'...
            'end,'...
            'userData = get(get(gcbo,''parent''),''UserData'');'...
            'XLocations = userData.XLocations;'...
            'YLocations = userData.YLocations;'...
            'OneColumnNewFileList = userData.OneColumnNewFileList;'...
            'ImageWidth = userData.ImageWidth;'...
            'ImageHeight = userData.ImageHeight;'...
            'SizeChange = userData.SizeChange;'...
            '[m,n] = size(XLocations);'...
            'if n > 1,'...
            'XLocations = XLocations'';'...
            'YLocations = YLocations'';'...
            'end,'...
            'Point1x = dsearchn(XLocations,Xcord(end-1));'...
            'Point1y = dsearchn(YLocations,Ycord(end-1));'...
            'Point2x = dsearchn(XLocations,Xcord(end));'...
            'Point2y = dsearchn(YLocations,Ycord(end));'...
            'GridCheck = [XLocations,YLocations];'...
            'RowCheck1 = [XLocations(Point1x),YLocations(Point1y)];'...
            'RowCheck2 = [XLocations(Point2x),YLocations(Point2y)];'...
            'for i = 1:length(XLocations),'...
            '    flag1 = sum(RowCheck1 == GridCheck(i,:));'...
            '    if flag1 == 2,'...
            '        ImageRow1 = i;'...
            '    end,'...
            '    flag2 = sum(RowCheck2 == GridCheck(i,:));'...
            '    if flag2 == 2,'...
            '        ImageRow2 = i;'...
            '    end,'...
            'end,'...
            'ImageName1 = OneColumnNewFileList(ImageRow1);'...
            'ImageName2 = OneColumnNewFileList(ImageRow2);'...
            'if ~strcmp(ImageName1,ImageName2),'...
            '    error(''Unfortunately, you must choose two points within the same image to view at high resolution (Tile module)'');'...
            'end,'...
            'try LoadedImage = imread(fullfile(userData.DefaultImageDirectory,char(ImageName1))); catch, [Filename,Pathname] = CPuigetfile(''*.*'',[''Please open the image named: '',char(ImageName1)],userData.DefaultImageDirectory); if Filename == 0, return, else LoadedImage = imread(fullfile(Pathname,Filename)); end, end,'...
            'pixelvalue = 1;'...
            'PreXImageNumber = 1;'...
            'while pixelvalue > 0,'...
            '    XImageNumber = PreXImageNumber;'...
            '    pixelvalue = Xcord(end) - PreXImageNumber*ImageWidth;'...
            '    PreXImageNumber = PreXImageNumber + 1;'...
            'end,'...
            'pixelvalue = 1;'...
            'PreYImageNumber = 1;'...
            'while pixelvalue > 0,'...
            '    YImageNumber = PreYImageNumber;'...
            '    pixelvalue = Ycord(end) - PreYImageNumber*ImageHeight;'...
            '    PreYImageNumber = PreYImageNumber + 1;'...
            'end,'...
            'if XImageNumber == 1,'...
            '    xMin = min(Xcord(end-1),Xcord(end));'...
            '    xMax = max(Xcord(end-1),Xcord(end));'...
            '    if YImageNumber == 1,'...
            '        yMin = min(Ycord(end-1),Ycord(end));'...
            '        yMax = max(Ycord(end-1),Ycord(end));'...
            '    else,'...
            '        Ycord(end) = Ycord(end) - ImageHeight*(YImageNumber-1);'...
            '        Ycord(end-1) = Ycord(end-1) - ImageHeight*(YImageNumber-1);'...
            '        yMin = min(Ycord(end-1),Ycord(end));'...
            '        yMax = max(Ycord(end-1),Ycord(end));'...
            '    end,'...
            'else,'...
            '    Xcord(end) = Xcord(end) - ImageWidth*(XImageNumber-1);'...
            '    Xcord(end-1) = Xcord(end-1) - ImageWidth*(XImageNumber-1);'...
            '    xMin = min(Xcord(end-1),Xcord(end));'...
            '    xMax = max(Xcord(end-1),Xcord(end));'...
            '    if YImageNumber == 1,'...
            '        yMin = min(Ycord(end-1),Ycord(end));'...
            '        yMax = max(Ycord(end-1),Ycord(end));'...
            '    else,'...
            '        Ycord(end) = Ycord(end) - ImageHeight*(YImageNumber-1);'...
            '        Ycord(end-1) = Ycord(end-1) - ImageHeight*(YImageNumber-1);'...
            '        yMin = min(Ycord(end-1),Ycord(end));'...
            '        yMax = max(Ycord(end-1),Ycord(end));'...
            '    end,'...
            'end,'...
            '[m,n] = size(LoadedImage);'...
            'BinaryCropImage = zeros(m,n);'...
            'BinaryCropImage(round(yMin/SizeChange):round(yMax/SizeChange),round(xMin/SizeChange):round(xMax/SizeChange)) = 1;'...
            'if any(size(LoadedImage(:,:,1)) ~= size(BinaryCropImage(:,:,1))),'...
            '    error([''Image processing was canceled in the '', ModuleName, '' module because an image you wanted to analyze is not the same size as the image used for cropping.  The pixel dimensions must be identical.'']),'...
            'end,'...
            'PrelimCroppedImage = LoadedImage;'...
            'ImagePixels = size(LoadedImage,1)*size(LoadedImage,2);'...
            'for Channel = 1:size(LoadedImage,3),'...
            '    PrelimCroppedImage((Channel-1)*ImagePixels + find(BinaryCropImage == 0)) = 0;'...
            'end,'...
            'drawnow,'...
            'ColumnTotals = sum(BinaryCropImage,1);'...
            'RowTotals = sum(BinaryCropImage,2);'...
            'warning off all,'...
            'ColumnsToDelete = ~logical(ColumnTotals);'...
            'RowsToDelete = ~logical(RowTotals);'...
            'warning on all,'...
            'drawnow,'...
            'CroppedImage = LoadedImage;'...
            'CroppedImage(:,ColumnsToDelete,:) = [];'...
            'CroppedImage(RowsToDelete,:,:) = [];'...
            'ScreenSize = get(0,''ScreenSize'');'...
            'Left = (ScreenSize(3)-250)/2;'...
            'Bottom = (ScreenSize(4)-442)/2;'...
            'FigHandle = figure(''color'',[.7 .7 .9],''position'',[Left Bottom 250 442]);'...
            'userData.Application = ''CellProfiler'';'...
            'set(FigHandle,''UserData'',userData);'...
            'colormap(gray);'...
            'subplot(2,1,1);'...
            'CPimagesc(LoadedImage,userData);'...
            'subplot(2,1,2);'...
            'CPimagesc(CroppedImage,userData);clear yMin yMax xMin xMax userData pixelvalue n m i flag2 flag1 Ycord YLocations YImageNumber Xcord XLocations XImageNumber SizeChange ScreenSize RowsToDelete RowTotals RowCheck1 RowCheck2 PrelimCroppedImage PreYImageNumber PreXImageNumber Point2y Point2x Point1y Point1x OneColumnNewFileList Oldtitle LoadedImage Left ImageWidth ImageRow2 ImageRow1 ImagePixels ImageName2 ImageName1 ImageHeight GridCheck FigHandle CroppedImage ColumnsToDelete ColumnTotals Channel Bottom BinaryCropImage ans'];
        uicontrol('Style', 'pushbutton', ...
            'String', 'HI-RES', 'Position', [290 6 55 20], 'BackgroundColor',[.7 .7 .9],...
            'Callback',HiResFunction,'parent',ThisModuleFigureNumber,'FontSize',FontSize);

        uicontrol('Parent',ThisModuleFigureNumber, ...
            'BackgroundColor',get(ThisModuleFigureNumber,'Color'), ...
            'Position',[10 28 95 14], ...
            'HorizontalAlignment','center', ...
            'String','Gridlines:', ...
            'Style','text', ...
            'FontSize',FontSize);
        uicontrol('Parent',ThisModuleFigureNumber, ...
            'BackgroundColor',get(ThisModuleFigureNumber,'Color'), ...
            'Position',[120 28 95 14], ...
            'HorizontalAlignment','center', ...
            'String','File names:', ...
            'Style','text', ...
            'FontSize',FontSize);
        uicontrol('Parent',ThisModuleFigureNumber, ...
            'BackgroundColor',get(ThisModuleFigureNumber,'Color'), ...
            'Position',[230 28 55 14], ...
            'HorizontalAlignment','center', ...
            'String','Colormap:', ...
            'Style','text', ...
            'FontSize',FontSize);

        %%% Draws the grid on the image.  The 0.5 accounts for the fact that
        %%% pixels are labeled where the middle of the pixel is a whole number,
        %%% and the left hand side of each pixel is 0.5.
        X(1:2,:) = [(0.5:ImageWidth:TotalWidth+0.5);(0.5:ImageWidth:TotalWidth+0.5)];
        NumberVerticalLines = size(X');
        NumberVerticalLines = NumberVerticalLines(1);
        Y(1,:) = repmat(0,1,NumberVerticalLines);
        Y(2,:) = repmat(TotalHeight,1,NumberVerticalLines);
        line(X,Y)

        NewY(1:2,:) = [(0.5:ImageHeight:TotalHeight+0.5);(0.5:ImageHeight:TotalHeight+0.5)];
        NumberHorizontalLines = size(NewY');
        NumberHorizontalLines = NumberHorizontalLines(1);
        NewX(1,:) = repmat(0,1,NumberHorizontalLines);
        NewX(2,:) = repmat(TotalWidth,1,NumberHorizontalLines);
        line(NewX,NewY)

        Handles = findobj('type','line');
        set(Handles, 'color',[.15 .15 .15])

        %%% Sets the location of Tick marks.
        set(gca, 'XTick', ImageWidth/2:ImageWidth:TotalWidth-ImageWidth/2)
        set(gca, 'YTick', ImageHeight/2:ImageHeight:TotalHeight-ImageHeight/2)

        %%% Sets the Tick Labels.
        if strcmp(LeftOrRight,'Right') == 1
            set(gca, 'XTickLabel',fliplr(1:NumberColumns))
        else
            set(gca, 'XTickLabel', 1:NumberColumns)
        end
        if strcmp(TopOrBottom,'Bottom') == 1
            set(gca, 'YTickLabel',fliplr(1:NumberRows))
        else
            set(gca, 'YTickLabel', 1:NumberRows)
        end

        %%% Calculates where to display the file names on the tiled image.
        %%% Provides the i,j coordinates of the file names.  The
        %%% cellfun(length) part is just a silly way to get a number for every
        %%% entry in the NewFileList so that the find function can find it.
        %%% find does not work directly on strings in cell arrays.
        [i,j] = find(cellfun('length',NewFileList));
        YLocations = i*ImageHeight - ImageHeight/2;
        XLocations = j*ImageWidth - ImageWidth/2;
        OneColumnNewFileList = reshape(NewFileList,[],1);
        PrintableOneColumnNewFileList = strrep(OneColumnNewFileList,'_','\_');
        %%% Creates FileNameText
        text(XLocations, YLocations, PrintableOneColumnNewFileList,...
            'HorizontalAlignment','center', 'color', 'white','visible','off', ...
            'UserData','FileNameTextHandles','fontsize',handles.Preferences.FontSize);
        userData = get(ThisModuleFigureNumber,'UserData');
        userData.XLocations = XLocations;
        userData.YLocations = YLocations;
        userData.OneColumnNewFileList = OneColumnNewFileList;
        userData.ImageWidth = ImageWidth;
        userData.ImageHeight = ImageHeight;
        userData.SizeChange = SizeChange;
        userData.Preferences.IntensityColorMap = handles.Preferences.IntensityColorMap;
        userData.Preferences.FontSize = handles.Preferences.FontSize;
        userData.DefaultImageDirectory = handles.Pipeline.(['Pathname',OrigImageName]);
        set(ThisModuleFigureNumber,'UserData',userData);
    end
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    %%% SAVE DATA TO HANDLES STRUCTURE %%%
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    drawnow

    %%% Saves the tiled image to the handles structure so it can be used by
    %%% subsequent modules.
    handles.Pipeline.(TiledImageName) = handles.Pipeline.TileData.(['Module' handles.Current.CurrentModuleNumber]).TiledImage;
else
    ThisModuleFigureNumber = handles.Current.(['FigureNumberForModule',CurrentModule]);
    if any(findobj == ThisModuleFigureNumber)
        CPfigure(handles,'Image',ThisModuleFigureNumber);
        title('Tiled image will be shown after the last image cycle only if this window is left open.');
    end
end