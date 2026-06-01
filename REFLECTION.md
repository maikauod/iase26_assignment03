# Reflection

**Prompt.** What classes of agent failures can `StubModelClient` not catch, and
what would you do instead?

<!-- TODO (~150 words). Name at least two concrete failure modes the stub
cannot reproduce. For each, describe how you would catch it in practice. -->
the implemented stubmodelclient checks a few important prerequisites and functionality however it's not nearly 
sufficient for the project. 
In simple terms it is just a substitute of a fake ai model used for testing that instead of sending prompts to a llm 
and generating a response, it responds with manual prewritten responses. 
While doing this it tries to record the logs of every prompt it received so its behaviour can be traced and analyzed. 
However it cannot return new/unexpected responses and adapt accordingly based on a prompt like most llms would. 
That means any context given isn’t integrated in the response and due to the limited nr of options to choose from the 
responses may often be faulty.
Moreover such a stubmodelclient isn’t the best tool to mirror/replicate environment failures/bugs in dev and prod 
because things like race conditions, network issues, i/o issues can go unnoticed by the agent. 
Overall it is good for testing predefined cases but imo doesn’t add much to a normal unittest suite
