def resizeString(String str, int lenght) {
    if (str.size() <= lenght) return str
    
    // Extract the first 80 characters
    def returnStr = str[0..lenght -1]

    // Check if the 81st character (index 80) exists and is not a space
    if (str.size() > lenght && str[lenght] != ' ') {
        // Find the last space in the extracted string to avoid cutting off a word
        int lastSpaceIndex = returnStr.lastIndexOf(' ')
        if (lastSpaceIndex != -1) {
            // Truncate up to the last space index if space is found
            returnStr = returnStr[0..lastSpaceIndex - 1]
        }
    }
    return returnStr
}


// Example

String loremIpsum1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent efficitur, orci non sodales facilisis, elit dolor scelerisque justo, a dictum mi quam id libero. Fusce sed laoreet enim." // n characters
String loremIpsum2 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do." // < 80 characters

println(resizeString(loremIpsum1, 90)) // Will be stripped to 90 characters or less.
println(resizeString(loremIpsum2, 90)) // Will still be < 80 characters.
