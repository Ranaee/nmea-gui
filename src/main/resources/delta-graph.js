window.onload = function () {

    document.querySelector("#read-delta-button").addEventListener('click', function () {
        if (document.querySelector("#delta-input").files.length == 0) {
            alert('Error : No file selected');
            return;
        }
        let file = document.querySelector("#delta-input").files[0];
        let inMeter = document.querySelector('#degrees').checked;

        // new FileReader object
        let reader = new FileReader();
        reader.addEventListener('load', function (e) {
            // contents of the file
            var deltaCSV = e.target.result;
            var latLabel = inMeter === false ? 'Latitude Delta (in degrees)' : 'Latitude Delta (in meters)'
            var xIndex = inMeter === false ? 0 : 3;
            var yIndex = inMeter === false ? 1 : 4;
            var chart = new CanvasJS.Chart("chartContainer4",
                {
                    zoomEnabled: true,
                    title: {
                        text: latLabel
                    },
                    data: [
                        {
                            type: "line",
                            xValueType: "dateTime",
                            dataPoints: getDataPointsFromDeltaCSV(deltaCSV, xIndex)
                        }]
                });
            chart.render();

            var longLabel = inMeter === false ? 'Longitude Delta (in degrees)' : 'Longitude Delta (in meters)'
            var chart2 = new CanvasJS.Chart("chartContainer5",
                {
                    zoomEnabled: true,
                    title: {
                        text: longLabel
                    },
                    data: [
                        {
                            type: "line",
                            xValueType: "dateTime",
                            dataPoints: getDataPointsFromDeltaCSV(deltaCSV, yIndex)
                        }]
                });
            chart2.render();

            var chart3 = new CanvasJS.Chart("chartContainer6",
                {
                    zoomEnabled: true,
                    title: {
                        text: 'Satellite count'
                    },
                    data: [
                        {
                            type: "line",
                            xValueType: "dateTime",
                            dataPoints: getDataPointsFromDeltaCSV(deltaCSV, 5)
                        }]
                });
            chart3.render();
        });

        // event fired when file reading failed
        reader.addEventListener('error', function () {
            alert('Error : Failed to read file');
        });

        // read file as text file
        reader.readAsText(file);

    });
}

function getDataPointsFromDopCSV(csv, dopIndex) {
    var dataPoints = csvLines = points = [];
    csvLines = csv.split(/[\r?\n|\r|\n]+/);

    for (var i = 1; i < csvLines.length; i++)
        if (csvLines[i].length > 0) {
            points = csvLines[i].split(",");
            dataPoints.push({
                x: parseFloat(points[3]),
                y: parseFloat(points[dopIndex])
            });
        }
    return dataPoints;
}

function getDataPointsFromDeltaCSV(csv, index) {
    var dataPoints = csvLines = points = [];
    csvLines = csv.split(/[\r?\n|\r|\n]+/);

    for (var i = 1; i < csvLines.length; i++)
        if (csvLines[i].length > 0) {
            points = csvLines[i].split(",");
            dataPoints.push({
                y: parseFloat(points[index])
            });
        }
    return dataPoints;
}