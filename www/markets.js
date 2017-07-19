module.exports = {
    Initialize: function(PublicKey, Market) {
		console.log("Initialize: " + PublicKey + ". Market: " + Market);
        cordova.exec(
			function (result) {
				console.log(result);
				alert(result);
			},
			function (error) {
				console.log(error);
				alert(JSON.stringify(error));
			},
            'MdMarkets',
            'Initialize',
            [PublicKey, Market]
        ); 
    },
    GetSkuDetails: function(ProductIds) {
		console.log("GetSkuDetails");
        cordova.exec(
			function (result) {
				console.log(result);
				alert(JSON.stringify(result));
			},
			function (error) {
				console.log(error);
				alert(JSON.stringify(error));
			},
            'MdMarkets',
            'GetSkuDetails',
            [ProductIds]
        ); 
    },
    RequestPayment: function(ProductId, Subscribe) {
		console.log("RequestPayment: " + ProductId);
        cordova.exec(
			function (result) {
				console.log(result);
				alert(JSON.stringify(result));
			},
			function (error) {
				console.log(error);
				alert(JSON.stringify(error));
			},
            'MdMarkets',
            'RequestPayment',
            [ProductId, Subscribe]
        ); 
    },
    GetOwnedProducts: function() {
		console.log("GetOwnedProducts");
        cordova.exec(
			function (result) {
				console.log(result);
				alert(JSON.stringify(result));
			},
			function (error) {
				console.log(error);
				alert(JSON.stringify(error));
			},
            'MdMarkets',
            'GetOwnedProducts',
            []
        ); 
    }
};