// Get values from variables and put them in the description
issue.description  = replica.description + "\n"

// This will set the name of the variabel with the value in the description
replica.variables.each { key, value ->
    issue.description += "${key} : ${value}\n"
}

// this will only add the variables you specify
replica.variables.each { key, value ->
    if (key == "<Var name (check entity sync status for the right name)>")
        issue.description += "${key} : ${value}\n"
}